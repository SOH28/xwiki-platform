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
package org.xwiki.ranking.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.ranking.RankingManager;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.SolrException;

/**
 * Solr core initializer for the ranking informations.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Component
@Singleton
@Named(RankingSolrCoreInitializer.DEFAULT_RANKING_SOLR_CORE)
public class RankingSolrCoreInitializer extends AbstractSolrCoreInitializer
{
    /**
     * Name of the Solr core for ranking.
     */
    public static final String DEFAULT_RANKING_SOLR_CORE = "ranking";

    private static final long CURRENT_VERSION = 120800000;

    @Override
    protected void createSchema() throws SolrException
    {
        this.addStringField(RankingManager.RankingQueryField.MANAGER_ID.getFieldName(), false, false);
        this.addStringField(RankingManager.RankingQueryField.ENTITY_REFERENCE.getFieldName(), false, false);
        this.addStringField(RankingManager.RankingQueryField.ENTITY_TYPE.getFieldName(), false, false);
        this.addStringField(RankingManager.RankingQueryField.USER_REFERENCE.getFieldName(), false, false);
        this.addPIntField(RankingManager.RankingQueryField.VOTE.getFieldName(), false, false);
        this.addPIntField(RankingManager.RankingQueryField.SCALE.getFieldName(), false, false);
        this.addPDateField(RankingManager.RankingQueryField.CREATED_DATE.getFieldName(), false, false);
        this.addPDateField(RankingManager.RankingQueryField.UPDATED_DATE.getFieldName(), false, false);
    }


    @Override
    protected void migrateSchema(long cversion) throws SolrException
    {
        // No migration yet.
    }

    @Override
    protected long getVersion()
    {
        return CURRENT_VERSION;
    }
}
