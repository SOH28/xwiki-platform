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
package org.xwiki.ranking;

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Manager for handling Ranking operations.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Role
@Unstable
public interface RankingManager
{
    /**
     * The fields to be used for performing queries on Rankings.
     */
    enum RankingQueryField
    {
        IDENTIFIER("id"),
        ENTITY_REFERENCE("rankedElement"),
        ENTITY_TYPE("entityType"),
        USER_REFERENCE("voter"),
        VOTE("vote"),
        CREATED_DATE("createdAt"),
        UPDATED_DATE("updatedAt"),
        MANAGER_ID("managerId"),
        SCALE("scale");

        private final String fieldName;

        RankingQueryField(String fieldName)
        {
            this.fieldName = fieldName;
        }

        public String getFieldName()
        {
            return this.fieldName;
        }
    }

    /**
     * @return the identifier of the current manager.
     */
    String getIdentifier();

    /**
     * Allows to set the identifier of the manager.
     * This method should only be used when creating the manager in a {@link RankingManagerFactory}.
     *
     * @param identifier the identifier to be set.
     */
    void setIdentifer(String identifier);

    /**
     * @return the upper bound of the scale used by this manager for ranking.
     */
    int getScale();

    /**
     * Allows to set the configuration of the manager.
     * This method should only be used when creating the manager in a {@link RankingManagerFactory}.
     *
     * @param configuration the configuration to be set.
     */
    void setRankingConfiguration(RankingConfiguration configuration);

    /**
     * @return the configuration used by this manager.
     */
    RankingConfiguration getRankingConfiguration();

    /**
     * Save and return a {@link Ranking} information.
     * If an existing rank has already been saved by the same user on the same reference, then this method updates the
     * existing value.
     * This method should check that the given vote matches the scale of the manager.
     * It should also take into account the {@link RankingConfiguration#storeZero()} configuration to handle case when
     * the vote is equal to 0. The method returns null if the vote is equal to 0 and the configuration doesn't allow
     * to store it, but it might perform storage side effect (such as removing a previous {@link Ranking} information).
     * This method also handles the computation of {@link AverageRank} if the
     * {@link RankingConfiguration#storeAverage()} configuration is set to true.
     * Note that this method should also handle sending the appropriate
     * {@link org.xwiki.ranking.events.CreatedRankingEvent} and {@link org.xwiki.ranking.events.UpdatedRankingEvent}.
     *
     * @param rankedEntity the entity for which to save a ranking value.
     * @param voter the user who performs the rank.
     * @param vote the actual rank to be saved.
     * @return the saved ranking or null if none has been saved.
     * @throws RankingException in case of problem for saving the ranking.
     */
    Ranking saveRank(EntityReference rankedEntity, UserReference voter, int vote) throws RankingException;

    /**
     * Retrieve the list of rankings based on the given query parameters.
     * Only exact matching can be used right now for the given query parameters. It's possible to provide some
     * objects as query parameters: some specific treatment can be apply depending on the type of the objects, but for
     * most type we're just relying on {@code String.valueOf(Object)}. Only the rankings of the current manager are
     * retrieved even if the store is shared.
     *
     * @param queryParameters the map of parameters to rely on for query the rankings.
     * @param offset the offset where to start getting results.
     * @param limit the limit number of results to retrieve.
     * @param orderBy the field to use for sorting the results.
     * @param asc if {@code true}, use ascending order for sorting, else use descending order.
     * @return a list containing at most {@code limit} rankings results.
     * @throws RankingException in case of problem for querying the rankings.
     */
    List<Ranking> getRankings(Map<RankingQueryField, Object> queryParameters,
        int offset, int limit, RankingQueryField orderBy, boolean asc) throws RankingException;

    /**
     * Retrieve the number of rankings matching the given parameters but without retrieving them directly.
     * Only exact matching can be used right now for the given query parameters. It's possible to provide some
     * objects as query parameters: some specific treatment can be apply depending on the type of the objects, but for
     * most type we're just relying on {@code String.valueOf(Object)}. Only the rankings of the current manager are
     * retrieved even if the store is shared.
     *
     * @param queryParameters the map of parameters to rely on for query the rankings.
     * @return the total number of rankings matching the query parameters.
     * @throws RankingException in case of problem during the query.
     */
    long countRankings(Map<RankingQueryField, Object> queryParameters) throws RankingException;

    /**
     * Remove a ranking based on its identifier.
     * This method also performs an update of the {@link AverageRank} if the {@link RankingConfiguration#storeAverage()}
     * is enabled.
     *
     * @param rankingIdentifier the ranking identifier to remove.
     * @return {@code true} if a ranking is deleted, {@code false} if no ranking with the given identifier can be found.
     * @throws RankingException in case of problem during the query.
     */
    boolean removeRanking(String rankingIdentifier) throws RankingException;

    /**
     * Retrieve the average rank information of the given reference.
     *
     * @param entityReference the reference for which to retrieve the average rank information.
     * @return the average rank data corresponding to the given reference.
     * @throws RankingException in case of problem during the query.
     */
    AverageRank getAverageRank(EntityReference entityReference) throws RankingException;
}
