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

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.ranking.AverageRank;
import org.xwiki.ranking.Ranking;
import org.xwiki.ranking.RankingConfiguration;
import org.xwiki.ranking.RankingException;
import org.xwiki.ranking.RankingManager;
import org.xwiki.ranking.events.CreatedRankingEvent;
import org.xwiki.ranking.events.DeletedRankingEvent;
import org.xwiki.ranking.events.UpdatedAverageRankEvent;
import org.xwiki.ranking.events.UpdatedRankingEvent;
import org.xwiki.ranking.internal.AverageRankSolrCoreInitializer.AverageRankField;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Default implementation of RankingManager which stores Rankings and AverageRanks in Solr.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Component
@Named("solr")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultRankingManager implements RankingManager
{
    @Inject
    private SolrUtils solrUtils;

    @Inject
    private Solr solr;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private ObservationManager observationManager;

    private RankingConfiguration rankingConfiguration;

    private String identifier;

    /**
     * Retrieve the solr client for storing rankings based on the configuration.
     * If the configuration specifies to use a dedicated core (see {@link RankingConfiguration#hasDedicatedCore()}),
     * then it will use a client based on the current manager identifier, else it will use the default solr core.
     *
     * @return the right solr client for storing rankings.
     * @throws SolrException in case of problem to retrieve the solr client.
     */
    private SolrClient getRankingSolrClient() throws SolrException
    {
        if (this.getRankingConfiguration().hasDedicatedCore()) {
            return this.solr.getClient(this.getIdentifier());
        } else {
            return this.solr.getClient(RankingSolrCoreInitializer.DEFAULT_RANKING_SOLR_CORE);
        }
    }

    private SolrClient getAverageRankSolrClient() throws SolrException
    {
        return this.solr.getClient(AverageRankSolrCoreInitializer.DEFAULT_AVERAGE_RANK_SOLR_CORE);
    }

    @Override
    public String getIdentifier()
    {
        return this.identifier;
    }

    @Override
    public void setIdentifer(String identifier)
    {
        this.identifier = identifier;
    }

    @Override
    public int getScale()
    {
        return this.getRankingConfiguration().getScale();
    }

    @Override
    public void setRankingConfiguration(RankingConfiguration configuration)
    {
        this.rankingConfiguration = configuration;
    }

    @Override
    public RankingConfiguration getRankingConfiguration()
    {
        return this.rankingConfiguration;
    }

    private SolrQuery.ORDER getOrder(boolean asc)
    {
        return (asc) ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
    }

    private Ranking getRankingFromSolrDocument(SolrDocument document)
    {
        String rankingId = this.solrUtils.getId(document);
        String managerId = this.solrUtils.get(RankingQueryField.MANAGER_ID.getFieldName(), document);
        String serializedEntityReference = this.solrUtils.get(RankingQueryField.ENTITY_REFERENCE.getFieldName(),
            document);
        String serializedUserReference = this.solrUtils.get(RankingQueryField.USER_REFERENCE.getFieldName(), document);
        int vote = this.solrUtils.get(RankingQueryField.VOTE.getFieldName(), document);
        Date createdAt = this.solrUtils.get(RankingQueryField.CREATED_DATE.getFieldName(), document);
        Date updatedAt = this.solrUtils.get(RankingQueryField.UPDATED_DATE.getFieldName(), document);
        int scale = this.solrUtils.get(RankingQueryField.SCALE.getFieldName(), document);
        String entityTypeValue = this.solrUtils.get(RankingQueryField.ENTITY_TYPE.getFieldName(), document);

        EntityType entityType = EntityType.valueOf(entityTypeValue);
        UserReference userReference = this.userReferenceResolver.resolve(serializedUserReference);
        EntityReference entityReference = this.entityReferenceResolver.resolve(serializedEntityReference, entityType);

        return new DefaultRanking(rankingId)
            .setRankedElement(entityReference)
            .setVoter(userReference)
            .setRank(vote)
            .setScale(scale)
            .setCreatedAt(createdAt)
            .setUpdatedAt(updatedAt)
            .setManagerId(managerId);
    }

    private List<Ranking> getRankingsFromQueryResult(SolrDocumentList documents)
    {
        if (documents != null) {
            return documents.stream().map(this::getRankingFromSolrDocument).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private String mapToQuery(Map<RankingQueryField, Object> originalParameters)
    {
        Map<RankingQueryField, Object> queryParameters = new LinkedHashMap<>(originalParameters);
        queryParameters.put(RankingQueryField.MANAGER_ID, this.getIdentifier());

        StringBuilder result = new StringBuilder();
        Iterator<Map.Entry<RankingQueryField, Object>> iterator = queryParameters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<RankingQueryField, Object> queryParameter = iterator.next();
            result.append("filter(");
            result.append(queryParameter.getKey().getFieldName());
            result.append(":");

            Object value = queryParameter.getValue();
            if (value instanceof String || value instanceof Date) {
                result.append(solrUtils.toFilterQueryString(value));
            } else if (value instanceof UserReference) {
                result.append(
                    solrUtils.toFilterQueryString(this.userReferenceSerializer.serialize((UserReference) value)));
            } else if (value instanceof EntityReference) {
                result.append(
                    solrUtils.toFilterQueryString(this.entityReferenceSerializer.serialize((EntityReference) value)));
            } else if (value != null) {
                result.append(value);
            }
            result.append(")");
            if (iterator.hasNext()) {
                result.append(" AND ");
            }
        }

        return result.toString();
    }

    private SolrInputDocument getInputDocumentFromRanking(Ranking ranking)
    {
        SolrInputDocument result = new SolrInputDocument();
        solrUtils.setId(ranking.getId(), result);
        solrUtils.set(RankingQueryField.ENTITY_REFERENCE.getFieldName(),
            this.entityReferenceSerializer.serialize(ranking.getRankedElement()), result);
        solrUtils.set(RankingQueryField.ENTITY_TYPE.getFieldName(), ranking.getEntityType().toString(), result);
        solrUtils.set(RankingQueryField.CREATED_DATE.getFieldName(), ranking.getCreatedAt(), result);
        solrUtils.set(RankingQueryField.UPDATED_DATE.getFieldName(), ranking.getUpdatedAt(), result);
        solrUtils.set(RankingQueryField.USER_REFERENCE.getFieldName(),
            this.userReferenceSerializer.serialize(ranking.getVoter()), result);
        solrUtils.set(RankingQueryField.SCALE.getFieldName(), ranking.getScale(), result);
        solrUtils.set(RankingQueryField.MANAGER_ID.getFieldName(), ranking.getManagerId(), result);
        solrUtils.set(RankingQueryField.VOTE.getFieldName(), ranking.getRank(), result);
        return result;
    }

    private SolrInputDocument getInputDocumentFromAverageRank(AverageRank averageRank)
    {
        SolrInputDocument result = new SolrInputDocument();
        solrUtils.setId(averageRank.getId(), result);
        solrUtils.set(AverageRankField.RANKED_ELEMENT.getFieldName(),
            this.entityReferenceSerializer.serialize(averageRank.getRankedElement()), result);
        solrUtils.set(AverageRankField.UPDATED_AT.getFieldName(), averageRank.getUpdatedAt(), result);
        solrUtils.set(AverageRankField.VOTE_NUMBER.getFieldName(), averageRank.getRankingNumber(), result);
        solrUtils.set(AverageRankField.SCALE.getFieldName(), averageRank.getScale(), result);
        solrUtils.set(AverageRankField.MANAGER_ID.getFieldName(), averageRank.getManagerId(), result);
        solrUtils.set(AverageRankField.AVERAGE.getFieldName(), averageRank.getAverage(), result);
        solrUtils.set(AverageRankField.ENTITY_TYPE.getFieldName(), averageRank.getEntityType(), result);
        return result;
    }

    private Optional<Ranking> retrieveExistingRanking(EntityReference rankedEntity, UserReference voter)
        throws RankingException
    {
        String serializedEntity = this.entityReferenceSerializer.serialize(rankedEntity);
        String serializedUserReference = this.userReferenceSerializer.serialize(voter);
        Map<RankingQueryField, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(RankingQueryField.ENTITY_REFERENCE, serializedEntity);
        queryMap.put(RankingQueryField.ENTITY_TYPE, rankedEntity.getType());
        queryMap.put(RankingQueryField.USER_REFERENCE, serializedUserReference);

        List<Ranking> rankings = this.getRankings(queryMap, 0, 1, RankingQueryField.CREATED_DATE, true);
        if (rankings.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(rankings.get(0));
        }
    }

    @Override
    public Ranking saveRank(EntityReference rankedEntity, UserReference voter, int vote)
        throws RankingException
    {
        // If the vote is outside the scope of the scale, we throw an exception immediately.
        if (vote < 0 || vote > this.getScale()) {
            throw new RankingException(String.format("The vote [%s] is out of scale [%s] for [%s] ranking manager.",
                vote, this.getScale(), this.getIdentifier()));
        }

        // Check if a vote for the same entity by the same user and on the same manager already exists.
        Optional<Ranking> existingRanking = this.retrieveExistingRanking(rankedEntity, voter);

        boolean storeAverage = this.getRankingConfiguration().storeAverage();
        Event event = null;
        Ranking result = null;
        AverageRank averageRank = null;
        Event averageEvent = null;

        // It's the first vote for the tuple entity, user, manager.
        if (!existingRanking.isPresent()) {

            // We only store the vote if it's not 0 or if the configuration allows to store 0
            if (vote != 0 || this.getRankingConfiguration().storeZero()) {
                result = new DefaultRanking(UUID.randomUUID().toString())
                    .setManagerId(this.getIdentifier())
                    .setRankedElement(rankedEntity)
                    .setCreatedAt(new Date())
                    .setUpdatedAt(new Date())
                    .setRank(vote)
                    .setScale(this.getScale())
                    .setVoter(voter);

                // it's a vote creation
                event = new CreatedRankingEvent();

                if (storeAverage) {
                    averageRank = this.getAverageRank(rankedEntity);
                    averageEvent =
                        new UpdatedAverageRankEvent(averageRank.getAverage(), averageRank.getRankingNumber());
                    averageRank.addVote(vote);
                }
            }

        // There was already a vote with the same information
        } else {
            Ranking oldRanking = existingRanking.get();

            // If the vote is not 0 or if we store zero, we just modify the existing vote
            if (vote != 0) {
                result = new DefaultRanking(oldRanking)
                    .setUpdatedAt(new Date())
                    .setRank(vote);

                // It's an update of a vote
                event = new UpdatedRankingEvent(oldRanking.getRank());
                if (storeAverage) {
                    averageRank = this.getAverageRank(rankedEntity);
                    averageEvent =
                        new UpdatedAverageRankEvent(averageRank.getAverage(), averageRank.getRankingNumber());
                    averageRank.updateVote(oldRanking.getRank(), vote);
                }
            // Else we remove it.
            } else if (this.rankingConfiguration.storeZero()) {
                this.removeRanking(oldRanking.getId());
            }
        }

        // If there's a vote to store (all cases except if the vote is 0 and we don't store it)
        if (result != null) {
            SolrInputDocument solrInputDocument = this.getInputDocumentFromRanking(result);
            try {
                // Store the new document in Solr
                this.getRankingSolrClient().add(solrInputDocument);
                this.getRankingSolrClient().commit();

                // Send the appropriate notification
                this.observationManager.notify(event, this.getIdentifier(), result);

                // If we store the average, we also compute the new informations for it.
                if (storeAverage) {
                    this.getAverageRankSolrClient().add(this.getInputDocumentFromAverageRank(averageRank));
                    this.getAverageRankSolrClient().commit();
                    this.observationManager.notify(averageEvent, this.getIdentifier(), averageRank);
                }
            } catch (SolrServerException | IOException | SolrException e) {
                throw new RankingException(
                    String.format("Error when storing rank information for entity [%s] with user [%s].",
                        rankedEntity, voter), e);
            }
        }
        return result;
    }

    @Override
    public List<Ranking> getRankings(Map<RankingQueryField, Object> queryParameters, int offset, int limit,
        RankingQueryField orderBy, boolean asc) throws RankingException
    {
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery(this.mapToQuery(queryParameters))
            .setStart(offset)
            .setRows(limit)
            .setSort(orderBy.getFieldName(), this.getOrder(asc));

        try {
            QueryResponse query = this.getRankingSolrClient().query(solrQuery);
            return this.getRankingsFromQueryResult(query.getResults());
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RankingException("Error while trying to get rankings", e);
        }
    }

    @Override
    public long countRankings(Map<RankingQueryField, Object> queryParameters) throws RankingException
    {
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery(this.mapToQuery(queryParameters))
            .setStart(0)
            .setRows(0);

        try {
            QueryResponse query = this.getRankingSolrClient().query(solrQuery);
            return query.getResults().getNumFound();
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RankingException("Error while trying to get count of rankings", e);
        }
    }

    @Override
    public boolean removeRanking(String rankingIdentifier) throws RankingException
    {
        Map<RankingManager.RankingQueryField, Object> queryMap = Collections
            .singletonMap(RankingQueryField.IDENTIFIER, rankingIdentifier);

        List<Ranking> rankings = this.getRankings(queryMap, 0, 1, RankingQueryField.CREATED_DATE, true);
        if (!rankings.isEmpty()) {
            try {
                this.getRankingSolrClient().deleteById(rankingIdentifier);
                this.getRankingSolrClient().commit();
                Ranking ranking = rankings.get(0);
                this.observationManager.notify(new DeletedRankingEvent(), this.getIdentifier(), ranking);
                if (this.getRankingConfiguration().storeAverage()) {
                    AverageRank averageRank = getAverageRank(ranking.getRankedElement());
                    UpdatedAverageRankEvent event = new UpdatedAverageRankEvent(averageRank.getAverage(),
                        averageRank.getRankingNumber());
                    averageRank.removeVote(ranking.getRank());
                    this.getAverageRankSolrClient().add(this.getInputDocumentFromAverageRank(averageRank));
                    this.getAverageRankSolrClient().commit();
                    this.observationManager.notify(event, this.getIdentifier(), averageRank);
                }
                return true;
            } catch (SolrServerException | IOException | SolrException e) {
                throw new RankingException("Error while removing ranking.", e);
            }
        } else {
            return false;
        }
    }

    @Override
    public AverageRank getAverageRank(EntityReference entityReference) throws RankingException
    {
        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery(String.format("filter(%s:%s) AND filter(%s:%s) AND filter(%s:%s)",
                AverageRankField.MANAGER_ID.getFieldName(), solrUtils.toFilterQueryString(this.getIdentifier()),
                AverageRankField.RANKED_ELEMENT.getFieldName(),
                solrUtils.toFilterQueryString(this.entityReferenceSerializer.serialize(entityReference)),
                AverageRankField.ENTITY_TYPE.getFieldName(), solrUtils.toFilterQueryString(entityReference.getType())))
            .setStart(0)
            .setRows(1)
            .setSort(AverageRankSolrCoreInitializer.AverageRankField.UPDATED_AT.getFieldName(), this.getOrder(true));

        try {
            QueryResponse query = this.getAverageRankSolrClient().query(solrQuery);
            AverageRank result;
            if (!query.getResults().isEmpty()) {
                SolrDocument solrDocument = query.getResults().get(0);

                result = new DefaultAverageRank(solrUtils.getId(solrDocument))
                    .setManagerId(solrUtils.get(AverageRankField.MANAGER_ID.getFieldName(), solrDocument))
                    .setAverageRank(solrUtils.get(AverageRankField.AVERAGE.getFieldName(), solrDocument))
                    .setRankedElement(entityReference)
                    .setRankingNumber(solrUtils.get(AverageRankField.VOTE_NUMBER.getFieldName(), solrDocument))
                    .setScale(solrUtils.get(AverageRankField.SCALE.getFieldName(), solrDocument))
                    .setUpdatedAt(solrUtils.get(AverageRankField.UPDATED_AT.getFieldName(), solrDocument));
            } else {
                result = new DefaultAverageRank(UUID.randomUUID().toString())
                    .setManagerId(this.getIdentifier())
                    .setScale(this.getScale())
                    .setRankedElement(entityReference)
                    .setAverageRank(0)
                    .setRankingNumber(0);
            }
            return result;
        } catch (SolrServerException | IOException | SolrException e) {
            throw new RankingException("Error while trying to get average ranking value.", e);
        }
    }
}
