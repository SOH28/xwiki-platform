/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.refactoring.internal.job;

import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.api.AbstractCheckRightsRequest;
import org.xwiki.refactoring.job.RefactoringJobs;

/**
 * A job that can restore entities.
 *
 * @version $Id$
 * @since 9.4RC1
 */
@Component
@Named(RefactoringJobs.RESTORE)
public class RestoreJob extends AbstractDeletedDocumentsJob
{
    @Override
    public String getType()
    {
        return RefactoringJobs.RESTORE;
    }

    @Override
    protected void handleDeletedDocuments(List<Long> idsDeletedDocuments, AbstractCheckRightsRequest request)
    {
        this.progressManager.pushLevelProgress(idsDeletedDocuments.size(), this);

        for (Long idToRestore : idsDeletedDocuments) {
            if (this.status.isCanceled()) {
                break;
            } else {
                this.progressManager.startStep(this);
                modelBridge.restoreDeletedDocument(idToRestore, request);
                this.progressManager.endStep(this);
            }
        }

        this.progressManager.popLevelProgress(this);
    }
}
