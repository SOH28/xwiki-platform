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
package org.xwiki.ranking.script;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ranking.Ranking;
import org.xwiki.ranking.RankingConfiguration;
import org.xwiki.ranking.RankingException;
import org.xwiki.ranking.RankingManager;
import org.xwiki.ranking.RankingManagerFactory;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service to manipulate rankings.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Component
@Singleton
@Named("ranking")
@Unstable
public class RankingScriptService implements ScriptService
{
    @Inject
    private RankingManagerFactory rankingManagerFactory;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private Logger logger;

    private UserReference getCurrentUserReference()
    {
        return this.userReferenceResolver.resolve(this.contextProvider.get().getUserReference());
    }

    /**
     * Allows to save a rank for the given reference, with the current user reference, by using the given manager hint.
     *
     * @param managerHint the hint of the manager to use for saving this rank. (see {@link RankingManagerFactory}).
     * @param reference the reference for which to save a rank.
     * @param rank the rank to save.
     * @return an optional containing the {@link Ranking} value, or empty in case of problem or if the rank is 0 and the
     *          configuration doesn't allow to save 0 values (see {@link RankingConfiguration#storeZero()}).
     */
    public Optional<Ranking> saveRank(String managerHint, EntityReference reference, int rank)
    {
        try {
            RankingManager rankingManager = this.rankingManagerFactory.getInstance(managerHint);
            Ranking ranking = rankingManager.saveRank(reference, this.getCurrentUserReference(), rank);
            if (ranking != null) {
                return Optional.of(ranking);
            }
        } catch (RankingException e) {
            logger.error("Error while trying to rank reference [{}].", reference, ExceptionUtils.getRootCause(e));
        }
        return Optional.empty();
    }

    /**
     * Retrieve rankings information for the given reference on the given manager.
     *
     * @param managerHint the hint of the manager to use for retrieving rank information.
     *                  (see {@link RankingManagerFactory}).
     * @param reference the reference for which to retrieve ranking information.
     * @param offset the offset at which to start for retrieving information.
     * @param limit the limit number of information to retrieve.
     * @return a list of rankings containing a maximum of {@code limit} values.
     */
    public List<Ranking> getRankings(String managerHint, EntityReference reference, int offset, int limit)
    {
        try {
            RankingManager rankingManager = this.rankingManagerFactory.getInstance(managerHint);
            Map<RankingManager.RankingQueryField, Object> queryParameters = new HashMap<>();
            queryParameters.put(RankingManager.RankingQueryField.ENTITY_REFERENCE, reference);
            queryParameters.put(RankingManager.RankingQueryField.ENTITY_TYPE, reference.getType());
            return rankingManager.getRankings(queryParameters, offset, limit,
                RankingManager.RankingQueryField.UPDATED_DATE, false);
        } catch (RankingException e) {
            logger.error("Error when getting rankins for reference [{}].", reference, ExceptionUtils.getRootCause(e));
            return Collections.emptyList();
        }
    }
}
