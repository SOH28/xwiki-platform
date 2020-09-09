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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.ranking.AverageRank;
import org.xwiki.ranking.Ranking;
import org.xwiki.ranking.RankingConfiguration;
import org.xwiki.ranking.RankingException;
import org.xwiki.ranking.RankingManager;
import org.xwiki.ranking.RankingManager.RankingQueryField;
import org.xwiki.ranking.events.CreatedRankingEvent;
import org.xwiki.ranking.events.DeletedRankingEvent;
import org.xwiki.ranking.events.UpdatedAverageRankEvent;
import org.xwiki.ranking.events.UpdatedRankingEvent;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.ranking.internal.AverageRankSolrCoreInitializer.AverageRankField;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultRankingManager}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@ComponentTest
public class DefaultRankingManagerTest
{
    @InjectMockComponents
    private DefaultRankingManager manager;

    @MockComponent
    private SolrUtils solrUtils;

    @MockComponent
    private Solr solr;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private EntityReferenceResolver<String> entityReferenceResolver;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private RankingConfiguration configuration;

    @Mock
    private SolrClient solrClient;

    @Mock
    private SolrDocumentList documentList;

    @BeforeEach
    void setup()
    {
        this.manager.setRankingConfiguration(configuration);
        when(this.solrUtils.toFilterQueryString(any()))
            .then(invocationOnMock -> invocationOnMock.getArgument(0).toString().replaceAll(":", "\\\\:"));
        when(this.solrUtils.getId(any()))
            .then(invocationOnMock -> ((SolrDocument) invocationOnMock.getArgument(0)).get("id"));
        when(this.solrUtils.get(any(), any()))
            .then(invocationOnMock ->
                ((SolrDocument) invocationOnMock.getArgument(1)).get((String) invocationOnMock.getArgument(0)));
        doAnswer(invocationOnMock -> {
            String fieldName = invocationOnMock.getArgument(0);
            Object fieldValue = invocationOnMock.getArgument(1);
            SolrInputDocument inputDocument = invocationOnMock.getArgument(2);
            inputDocument.setField(fieldName, fieldValue);
            return null;
        }).when(this.solrUtils).set(any(), any(Object.class), any());
        doAnswer(invocationOnMock -> {
            Object fieldValue = invocationOnMock.getArgument(0);
            SolrInputDocument inputDocument = invocationOnMock.getArgument(1);
            inputDocument.setField("id", fieldValue);
            return null;
        }).when(this.solrUtils).setId(any(), any());
    }

    private QueryResponse prepareSolrClientQueryWhenStatement(SolrClient solrClient, SolrQuery expectedQuery)
        throws Exception
    {
        QueryResponse response = mock(QueryResponse.class);
        when(solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery givenQuery = invocationOnMock.getArgument(0);
            assertEquals(expectedQuery.getQuery(), givenQuery.getQuery());
            assertArrayEquals(expectedQuery.getFilterQueries(), givenQuery.getFilterQueries());
            assertEquals(expectedQuery.getRows(), givenQuery.getRows());
            assertEquals(expectedQuery.getStart(), givenQuery.getStart());
            assertEquals(expectedQuery.getSorts(), givenQuery.getSorts());
            return response;
        });
        return response;
    }

    @Test
    void countRankings() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        EntityReference reference = new EntityReference("toto", EntityType.BLOCK);
        Map<RankingManager.RankingQueryField, Object> queryParameters = new LinkedHashMap<>();
        queryParameters.put(RankingQueryField.ENTITY_REFERENCE, reference);
        queryParameters.put(RankingQueryField.USER_REFERENCE, userReference);
        queryParameters.put(RankingQueryField.SCALE, 12);

        String managerId = "managerTest";
        this.manager.setIdentifer(managerId);
        when(this.configuration.hasDedicatedCore()).thenReturn(true);
        when(this.solr.getClient(managerId)).thenReturn(this.solrClient);

        when(this.entityReferenceSerializer.serialize(reference)).thenReturn("block:toto");
        when(this.userReferenceSerializer.serialize(userReference)).thenReturn("user:Foobar");
        String query = "filter(rankedElement:block\\:toto) AND filter(voter:user\\:Foobar) "
            + "AND filter(scale:12) AND filter(managerId:managerTest)";
        SolrQuery expectedQuery = new SolrQuery().addFilterQuery(query).setStart(0).setRows(0);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);
        when(response.getResults()).thenReturn(this.documentList);
        when(this.documentList.getNumFound()).thenReturn(455L);

        assertEquals(455L, this.manager.countRankings(queryParameters));
    }

    @Test
    void getRankings() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        Map<RankingManager.RankingQueryField, Object> queryParameters = new LinkedHashMap<>();
        queryParameters.put(RankingQueryField.ENTITY_TYPE, EntityType.PAGE_ATTACHMENT);
        queryParameters.put(RankingQueryField.USER_REFERENCE, userReference);
        queryParameters.put(RankingQueryField.SCALE, "6");

        String managerId = "otherId";
        this.manager.setIdentifer(managerId);
        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RankingSolrCoreInitializer.DEFAULT_RANKING_SOLR_CORE)).thenReturn(this.solrClient);

        when(this.userReferenceSerializer.serialize(userReference)).thenReturn("user:barfoo");
        when(this.userReferenceResolver.resolve("user:barfoo")).thenReturn(userReference);
        String query = "filter(entityType:PAGE_ATTACHMENT) AND filter(voter:user\\:barfoo) "
            + "AND filter(scale:6) AND filter(managerId:otherId)";

        int offset = 12;
        int limit = 42;
        String orderField = "voter";
        boolean asc = false;
        SolrQuery expectedQuery = new SolrQuery().addFilterQuery(query)
            .setStart(offset)
            .setRows(limit)
            .addSort(orderField, SolrQuery.ORDER.desc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);
        when(response.getResults()).thenReturn(this.documentList);

        Map<String, Object> documentResult = new HashMap<>();
        documentResult.put("id", "result1");
        documentResult.put(RankingQueryField.ENTITY_TYPE.getFieldName(), "PAGE_ATTACHMENT");
        documentResult.put(RankingQueryField.MANAGER_ID.getFieldName(), "otherId");
        documentResult.put(RankingQueryField.CREATED_DATE.getFieldName(), new Date(1));
        documentResult.put(RankingQueryField.UPDATED_DATE.getFieldName(), new Date(1111));
        documentResult.put(RankingQueryField.VOTE.getFieldName(), 8);
        documentResult.put(RankingQueryField.SCALE.getFieldName(), 10);
        documentResult.put(RankingQueryField.ENTITY_REFERENCE.getFieldName(), "attachment:Foo");
        EntityReference reference1 = new EntityReference("Foo", EntityType.PAGE_ATTACHMENT);
        when(this.entityReferenceResolver.resolve("attachment:Foo", EntityType.PAGE_ATTACHMENT)).thenReturn(reference1);
        documentResult.put(RankingQueryField.USER_REFERENCE.getFieldName(), "user:barfoo");
        SolrDocument result1 = new SolrDocument(documentResult);

        documentResult = new HashMap<>();
        documentResult.put("id", "result2");
        documentResult.put(RankingQueryField.ENTITY_TYPE.getFieldName(), "PAGE_ATTACHMENT");
        documentResult.put(RankingQueryField.MANAGER_ID.getFieldName(), "otherId");
        documentResult.put(RankingQueryField.CREATED_DATE.getFieldName(), new Date(2));
        documentResult.put(RankingQueryField.UPDATED_DATE.getFieldName(), new Date(2222));
        documentResult.put(RankingQueryField.VOTE.getFieldName(), 1);
        documentResult.put(RankingQueryField.SCALE.getFieldName(), 10);
        documentResult.put(RankingQueryField.ENTITY_REFERENCE.getFieldName(), "attachment:Bar");
        EntityReference reference2 = new EntityReference("Bar", EntityType.PAGE_ATTACHMENT);
        when(this.entityReferenceResolver.resolve("attachment:Bar", EntityType.PAGE_ATTACHMENT)).thenReturn(reference2);
        documentResult.put(RankingQueryField.USER_REFERENCE.getFieldName(), "user:barfoo");
        SolrDocument result2 = new SolrDocument(documentResult);

        documentResult = new HashMap<>();
        documentResult.put("id", "result3");
        documentResult.put(RankingQueryField.ENTITY_TYPE.getFieldName(), "PAGE_ATTACHMENT");
        documentResult.put(RankingQueryField.MANAGER_ID.getFieldName(), "otherId");
        documentResult.put(RankingQueryField.CREATED_DATE.getFieldName(), new Date(3));
        documentResult.put(RankingQueryField.UPDATED_DATE.getFieldName(), new Date(3333));
        documentResult.put(RankingQueryField.VOTE.getFieldName(), 3);
        documentResult.put(RankingQueryField.SCALE.getFieldName(), 10);
        documentResult.put(RankingQueryField.ENTITY_REFERENCE.getFieldName(), "attachment:Baz");
        EntityReference reference3 = new EntityReference("Baz", EntityType.PAGE_ATTACHMENT);
        when(this.entityReferenceResolver.resolve("attachment:Baz", EntityType.PAGE_ATTACHMENT)).thenReturn(reference3);
        documentResult.put(RankingQueryField.USER_REFERENCE.getFieldName(), "user:barfoo");
        SolrDocument result3 = new SolrDocument(documentResult);

        when(this.documentList.stream()).thenReturn(Arrays.asList(result1, result2, result3).stream());

        List<Ranking> expectedRankings = Arrays.asList(
            new DefaultRanking("result1")
                .setManagerId("otherId")
                .setCreatedAt(new Date(1))
                .setUpdatedAt(new Date(1111))
                .setRank(8)
                .setRankedElement(reference1)
                .setVoter(userReference)
                .setScale(10),

            new DefaultRanking("result2")
                .setManagerId("otherId")
                .setCreatedAt(new Date(2))
                .setUpdatedAt(new Date(2222))
                .setRank(1)
                .setRankedElement(reference2)
                .setVoter(userReference)
                .setScale(10),

            new DefaultRanking("result3")
                .setManagerId("otherId")
                .setCreatedAt(new Date(3))
                .setUpdatedAt(new Date(3333))
                .setRank(3)
                .setRankedElement(reference3)
                .setVoter(userReference)
                .setScale(10)
        );
        assertEquals(expectedRankings,
            this.manager.getRankings(queryParameters, offset, limit, RankingQueryField.USER_REFERENCE, asc));
    }

    @Test
    void getAverageRankNotExisting() throws Exception
    {
        when(this.solr.getClient(AverageRankSolrCoreInitializer.DEFAULT_AVERAGE_RANK_SOLR_CORE))
            .thenReturn(this.solrClient);

        String managerId = "averageId1";
        this.manager.setIdentifer(managerId);
        when(this.configuration.getScale()).thenReturn(7);
        EntityReference reference = new EntityReference("xwiki:FooBarBar", EntityType.PAGE);
        when(this.entityReferenceSerializer.serialize(reference)).thenReturn("xwiki:FooBarBar");

        String filterQuery = "filter(managerId:averageId1) AND filter(rankedElement:xwiki\\:FooBarBar) "
            + "AND filter(entityType:PAGE)";
        SolrQuery solrQuery = new SolrQuery().addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .setSort("updatedAt", SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, solrQuery);
        when(response.getResults()).thenReturn(this.documentList);
        when(this.documentList.isEmpty()).thenReturn(true);
        AverageRank averageRank = this.manager.getAverageRank(reference);

        AverageRank expectedRank = new DefaultAverageRank(averageRank.getId())
            .setAverageRank(0)
            .setRankingNumber(0)
            .setUpdatedAt(averageRank.getUpdatedAt())
            .setManagerId(managerId)
            .setScale(7)
            .setRankedElement(reference);
        assertEquals(expectedRank, averageRank);
    }

    @Test
    void getAverageRankExisting() throws Exception
    {
        when(this.solr.getClient(AverageRankSolrCoreInitializer.DEFAULT_AVERAGE_RANK_SOLR_CORE))
            .thenReturn(this.solrClient);

        String managerId = "averageId2";
        this.manager.setIdentifer(managerId);
        EntityReference reference = new EntityReference("xwiki:Something", EntityType.PAGE);
        when(this.entityReferenceSerializer.serialize(reference)).thenReturn("xwiki:Something");
        when(this.entityReferenceResolver.resolve("xwiki:Something", EntityType.PAGE)).thenReturn(reference);

        String filterQuery = "filter(managerId:averageId2) AND filter(rankedElement:xwiki\\:Something) "
            + "AND filter(entityType:PAGE)";
        SolrQuery solrQuery = new SolrQuery().addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .setSort("updatedAt", SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, solrQuery);
        when(response.getResults()).thenReturn(this.documentList);
        when(this.documentList.isEmpty()).thenReturn(false);

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("id", "average1");
        fieldMap.put(AverageRankField.AVERAGE.getFieldName(), 2.341);
        fieldMap.put(AverageRankField.VOTE_NUMBER.getFieldName(), 242L);
        fieldMap.put(AverageRankField.RANKED_ELEMENT.getFieldName(), "xwiki:Something");
        fieldMap.put(AverageRankField.ENTITY_TYPE.getFieldName(), "PAGE");
        fieldMap.put(AverageRankField.MANAGER_ID.getFieldName(), managerId);
        fieldMap.put(AverageRankField.UPDATED_AT.getFieldName(), new Date(42));
        fieldMap.put(AverageRankField.SCALE.getFieldName(), 12);
        SolrDocument solrDocument = new SolrDocument(fieldMap);
        when(this.documentList.get(0)).thenReturn(solrDocument);

        AverageRank averageRank = this.manager.getAverageRank(reference);

        AverageRank expectedRank = new DefaultAverageRank("average1")
            .setAverageRank(2.341)
            .setRankingNumber(242L)
            .setUpdatedAt(new Date(42))
            .setManagerId(managerId)
            .setScale(12)
            .setRankedElement(reference);
        assertEquals(expectedRank, averageRank);
    }

    @Test
    void removeRankingNotExisting() throws Exception
    {
        String rankingId = "ranking389";
        String managerId = "removeRanking1";
        this.manager.setIdentifer(managerId);
        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RankingSolrCoreInitializer.DEFAULT_RANKING_SOLR_CORE)).thenReturn(this.solrClient);

        String query = "filter(id:ranking389) AND filter(managerId:removeRanking1)";
        SolrQuery expectedQuery = new SolrQuery()
            .addFilterQuery(query)
            .setStart(0)
            .setRows(1)
            .setSort("createdAt", SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);
        when(response.getResults()).thenReturn(this.documentList);
        assertFalse(this.manager.removeRanking(rankingId));
        verify(this.solrClient, never()).deleteById(any(String.class));
    }

    @Test
    void removeRankingExisting() throws Exception
    {
        String rankingId = "ranking429";
        String managerId = "removeRanking2";
        this.manager.setIdentifer(managerId);
        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RankingSolrCoreInitializer.DEFAULT_RANKING_SOLR_CORE)).thenReturn(this.solrClient);

        String query = "filter(id:ranking429) AND filter(managerId:removeRanking2)";
        SolrQuery expectedQuery = new SolrQuery()
            .addFilterQuery(query)
            .setStart(0)
            .setRows(1)
            .setSort("createdAt", SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);
        when(response.getResults()).thenReturn(this.documentList);
        when(this.documentList.isEmpty()).thenReturn(false);

        Map<String, Object> documentResult = new HashMap<>();
        documentResult.put("id", rankingId);
        documentResult.put(RankingQueryField.ENTITY_TYPE.getFieldName(), "PAGE_ATTACHMENT");
        documentResult.put(RankingQueryField.MANAGER_ID.getFieldName(), managerId);
        documentResult.put(RankingQueryField.CREATED_DATE.getFieldName(), new Date(1));
        documentResult.put(RankingQueryField.UPDATED_DATE.getFieldName(), new Date(1111));
        documentResult.put(RankingQueryField.VOTE.getFieldName(), 8);
        documentResult.put(RankingQueryField.SCALE.getFieldName(), 10);
        documentResult.put(RankingQueryField.ENTITY_REFERENCE.getFieldName(), "attachment:Foo");
        EntityReference reference1 = new EntityReference("Foo", EntityType.PAGE_ATTACHMENT);
        when(this.entityReferenceResolver.resolve("attachment:Foo", EntityType.PAGE_ATTACHMENT)).thenReturn(reference1);
        when(this.entityReferenceSerializer.serialize(reference1)).thenReturn("attachment:Foo");
        documentResult.put(RankingQueryField.USER_REFERENCE.getFieldName(), "user:barfoo");
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve("user:barfoo")).thenReturn(userReference);
        SolrDocument result1 = new SolrDocument(documentResult);
        when(this.documentList.stream()).thenReturn(Collections.singletonList(result1).stream());

        Ranking ranking = new DefaultRanking(rankingId)
            .setRankedElement(reference1)
            .setManagerId(managerId)
            .setCreatedAt(new Date(1))
            .setUpdatedAt(new Date(1111))
            .setVoter(userReference)
            .setScale(10)
            .setRank(8);

        // Average rank handling
        when(this.configuration.storeAverage()).thenReturn(true);
        SolrClient averageSolrClient = mock(SolrClient.class);
        when(this.solr.getClient(AverageRankSolrCoreInitializer.DEFAULT_AVERAGE_RANK_SOLR_CORE))
            .thenReturn(averageSolrClient);

        String filterQuery = "filter(managerId:removeRanking2) AND filter(rankedElement:attachment\\:Foo) "
            + "AND filter(entityType:PAGE_ATTACHMENT)";
        SolrQuery averageQuery = new SolrQuery().addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .setSort("updatedAt", SolrQuery.ORDER.asc);
        QueryResponse averageResponse = prepareSolrClientQueryWhenStatement(averageSolrClient, averageQuery);
        SolrDocumentList averageDocumentList = mock(SolrDocumentList.class);
        when(averageResponse.getResults()).thenReturn(averageDocumentList);
        when(averageDocumentList.isEmpty()).thenReturn(false);

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("id", "average1");
        fieldMap.put(AverageRankField.AVERAGE.getFieldName(), 5.0);
        fieldMap.put(AverageRankField.VOTE_NUMBER.getFieldName(), 2L);
        fieldMap.put(AverageRankField.RANKED_ELEMENT.getFieldName(), "attachment:Foo");
        fieldMap.put(AverageRankField.ENTITY_TYPE.getFieldName(), "PAGE_ATTACHMENT");
        fieldMap.put(AverageRankField.MANAGER_ID.getFieldName(), managerId);
        fieldMap.put(AverageRankField.UPDATED_AT.getFieldName(), new Date(42));
        fieldMap.put(AverageRankField.SCALE.getFieldName(), 10);
        SolrDocument solrDocument = new SolrDocument(fieldMap);
        when(averageDocumentList.get(0)).thenReturn(solrDocument);

        DefaultAverageRank expectedModifiedAverage = new DefaultAverageRank("average1")
            .setAverageRank(2)
            .setRankingNumber(1)
            .setManagerId(managerId)
            .setRankedElement(reference1)
            .setScale(10);
        SolrInputDocument expectedInputDocument = new SolrInputDocument();
        expectedInputDocument.setField("id", "average1");
        expectedInputDocument.setField(AverageRankField.RANKED_ELEMENT.getFieldName(), "attachment:Foo");
        expectedInputDocument.setField(AverageRankField.UPDATED_AT.getFieldName(), new Date(0));
        expectedInputDocument.setField(AverageRankField.VOTE_NUMBER.getFieldName(), 1L);
        expectedInputDocument.setField(AverageRankField.SCALE.getFieldName(), 10);
        expectedInputDocument.setField(AverageRankField.MANAGER_ID.getFieldName(), managerId);
        expectedInputDocument.setField(AverageRankField.AVERAGE.getFieldName(), 2.0);
        expectedInputDocument.setField(AverageRankField.ENTITY_TYPE.getFieldName(), "PAGE_ATTACHMENT");

        when(averageSolrClient.add(any(SolrInputDocument.class))).then(invocationOnMock -> {
            SolrInputDocument obtainedInputDocument = invocationOnMock.getArgument(0);
            Date updatedAt = (Date) obtainedInputDocument.getFieldValue("updatedAt");
            expectedInputDocument.setField(AverageRankField.UPDATED_AT.getFieldName(), updatedAt);
            expectedModifiedAverage.setUpdatedAt(updatedAt);
            // We rely on the toString method since there's no proper equals method
            assertEquals(expectedInputDocument.toString(), obtainedInputDocument.toString());
            return null;
        });

        assertTrue(this.manager.removeRanking(rankingId));
        verify(this.solrClient).deleteById(rankingId);
        verify(this.solrClient).commit();
        verify(this.observationManager).notify(any(DeletedRankingEvent.class), eq(managerId), eq(ranking));
        verify(this.configuration).storeAverage();
        verify(averageSolrClient).add(any(SolrInputDocument.class));
        verify(averageSolrClient).commit();
        verify(this.observationManager).notify(new UpdatedAverageRankEvent(5, 2), managerId, expectedModifiedAverage);
    }

    @Test
    void saveRankOutScale()
    {
        when(this.configuration.getScale()).thenReturn(5);
        this.manager.setIdentifer("saveRank1");
        RankingException exception = assertThrows(RankingException.class, () -> {
            this.manager.saveRank(new EntityReference("test", EntityType.PAGE), mock(UserReference.class), -1);
        });
        assertEquals("The vote [-1] is out of scale [5] for [saveRank1] ranking manager.", exception.getMessage());

        exception = assertThrows(RankingException.class, () -> {
            this.manager.saveRank(new EntityReference("test", EntityType.PAGE), mock(UserReference.class), 8);
        });
        assertEquals("The vote [8] is out of scale [5] for [saveRank1] ranking manager.", exception.getMessage());
    }

    @Test
    void saveRankZeroNotExisting() throws Exception
    {
        String managerId = "saveRank2";
        this.manager.setIdentifer(managerId);
        int scale = 10;
        when(this.configuration.getScale()).thenReturn(scale);
        EntityReference reference = new EntityReference("foobar", EntityType.PAGE);
        when(this.entityReferenceSerializer.serialize(reference)).thenReturn("wiki:foobar");
        when(this.entityReferenceResolver.resolve("wiki:foobar", EntityType.PAGE)).thenReturn(reference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceSerializer.serialize(userReference)).thenReturn("user:Toto");
        when(this.userReferenceResolver.resolve("user:Toto")).thenReturn(userReference);

        String filterQuery = "filter(rankedElement:wiki\\:foobar) AND filter(entityType:PAGE) "
            + "AND filter(voter:user\\:Toto) AND filter(managerId:saveRank2)";
        SolrQuery expectedQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .addSort("createdAt", SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);

        // We don't mock stream behaviour, so that the returned result is empty.
        when(response.getResults()).thenReturn(this.documentList);

        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RankingSolrCoreInitializer.DEFAULT_RANKING_SOLR_CORE)).thenReturn(this.solrClient);

        when(this.configuration.storeZero()).thenReturn(false);
        assertNull(this.manager.saveRank(reference, userReference, 0));

        // Handle Average rank
        when(this.configuration.storeAverage()).thenReturn(true);
        SolrClient averageSolrClient = mock(SolrClient.class);
        when(this.solr.getClient(AverageRankSolrCoreInitializer.DEFAULT_AVERAGE_RANK_SOLR_CORE))
            .thenReturn(averageSolrClient);

        filterQuery = "filter(managerId:saveRank2) AND filter(rankedElement:wiki\\:foobar) "
            + "AND filter(entityType:PAGE)";
        SolrQuery averageQuery = new SolrQuery().addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .setSort("updatedAt", SolrQuery.ORDER.asc);
        QueryResponse averageResponse = prepareSolrClientQueryWhenStatement(averageSolrClient, averageQuery);
        SolrDocumentList averageDocumentList = mock(SolrDocumentList.class);
        when(averageResponse.getResults()).thenReturn(averageDocumentList);
        when(averageDocumentList.isEmpty()).thenReturn(false);

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("id", "average1");
        fieldMap.put(AverageRankField.AVERAGE.getFieldName(), 6.0);
        fieldMap.put(AverageRankField.VOTE_NUMBER.getFieldName(), 2L);
        fieldMap.put(AverageRankField.RANKED_ELEMENT.getFieldName(), "wiki:foobar");
        fieldMap.put(AverageRankField.ENTITY_TYPE.getFieldName(), "PAGE");
        fieldMap.put(AverageRankField.MANAGER_ID.getFieldName(), managerId);
        fieldMap.put(AverageRankField.UPDATED_AT.getFieldName(), new Date(42));
        fieldMap.put(AverageRankField.SCALE.getFieldName(), scale);
        SolrDocument solrDocument = new SolrDocument(fieldMap);
        when(averageDocumentList.get(0)).thenReturn(solrDocument);

        DefaultAverageRank expectedModifiedAverage = new DefaultAverageRank("average1")
            .setAverageRank(4)
            .setRankingNumber(3)
            .setManagerId(managerId)
            .setRankedElement(reference)
            .setScale(10);
        SolrInputDocument expectedAverageInputDocument = new SolrInputDocument();
        expectedAverageInputDocument.setField("id", "average1");
        expectedAverageInputDocument.setField(AverageRankField.RANKED_ELEMENT.getFieldName(), "wiki:foobar");
        expectedAverageInputDocument.setField(AverageRankField.UPDATED_AT.getFieldName(), new Date(0));
        expectedAverageInputDocument.setField(AverageRankField.VOTE_NUMBER.getFieldName(), 3L);
        expectedAverageInputDocument.setField(AverageRankField.SCALE.getFieldName(), scale);
        expectedAverageInputDocument.setField(AverageRankField.MANAGER_ID.getFieldName(), managerId);
        expectedAverageInputDocument.setField(AverageRankField.AVERAGE.getFieldName(), 4.0);
        expectedAverageInputDocument.setField(AverageRankField.ENTITY_TYPE.getFieldName(), "PAGE");

        when(averageSolrClient.add(any(SolrInputDocument.class))).then(invocationOnMock -> {
            SolrInputDocument obtainedInputDocument = invocationOnMock.getArgument(0);
            Date updatedAt = (Date) obtainedInputDocument.getFieldValue("updatedAt");
            expectedAverageInputDocument.setField(AverageRankField.UPDATED_AT.getFieldName(), updatedAt);
            expectedModifiedAverage.setUpdatedAt(updatedAt);
            // We rely on the toString method since there's no proper equals method
            assertEquals(expectedAverageInputDocument.toString(), obtainedInputDocument.toString());
            return null;
        });

        DefaultRanking expectedRanking = new DefaultRanking("")
            .setManagerId(managerId)
            .setRankedElement(reference)
            .setCreatedAt(new Date())
            .setUpdatedAt(new Date())
            .setRank(0)
            .setScale(scale)
            .setVoter(userReference);

        SolrInputDocument expectedInputDocument = new SolrInputDocument();
        expectedInputDocument.setField("id", "");
        expectedInputDocument.setField(RankingQueryField.ENTITY_REFERENCE.getFieldName(), "wiki:foobar");
        expectedInputDocument.setField(RankingQueryField.ENTITY_TYPE.getFieldName(), "PAGE");
        expectedInputDocument.setField(RankingQueryField.CREATED_DATE.getFieldName(), new Date());
        expectedInputDocument.setField(RankingQueryField.UPDATED_DATE.getFieldName(), new Date());
        expectedInputDocument.setField(RankingQueryField.USER_REFERENCE.getFieldName(), "user:Toto");
        expectedInputDocument.setField(RankingQueryField.SCALE.getFieldName(), scale);
        expectedInputDocument.setField(RankingQueryField.MANAGER_ID.getFieldName(), managerId);
        expectedInputDocument.setField(RankingQueryField.VOTE.getFieldName(), 0);

        when(this.solrClient.add(any(SolrInputDocument.class))).then(invocationOnMock -> {
            SolrInputDocument obtainedInputDocument = invocationOnMock.getArgument(0);
            Date updatedAt = (Date) obtainedInputDocument.getFieldValue("updatedAt");
            Date createdAt = (Date) obtainedInputDocument.getFieldValue("createdAt");
            String id = (String) obtainedInputDocument.getFieldValue("id");
            expectedInputDocument.setField(RankingQueryField.CREATED_DATE.getFieldName(), createdAt);
            expectedInputDocument.setField(RankingQueryField.UPDATED_DATE.getFieldName(), updatedAt);
            expectedInputDocument.setField("id", id);

            expectedRanking
                .setId(id)
                .setCreatedAt(createdAt)
                .setUpdatedAt(updatedAt);
            // We rely on the toString method since there's no proper equals method
            assertEquals(expectedInputDocument.toString(), obtainedInputDocument.toString());
            return null;
        });

        when(this.configuration.storeZero()).thenReturn(true);
        assertEquals(expectedRanking, this.manager.saveRank(reference, userReference, 0));
        verify(this.solrClient).add(any(SolrInputDocument.class));
        verify(this.solrClient).commit();
        verify(this.observationManager).notify(any(CreatedRankingEvent.class), eq(managerId), eq(expectedRanking));
        verify(averageSolrClient).add(any(SolrInputDocument.class));
        verify(averageSolrClient).commit();
        verify(this.observationManager)
            .notify(any(UpdatedAverageRankEvent.class), eq(managerId), eq(expectedModifiedAverage));
    }

    @Test
    void saveRankExisting() throws Exception
    {
        String managerId = "saveRank3";
        this.manager.setIdentifer(managerId);
        int scale = 8;
        int newVote = 2;
        int oldVote = 3;
        when(this.configuration.getScale()).thenReturn(scale);
        EntityReference reference = new EntityReference("foobar", EntityType.PAGE);
        when(this.entityReferenceSerializer.serialize(reference)).thenReturn("wiki:foobar");
        when(this.entityReferenceResolver.resolve("wiki:foobar", EntityType.PAGE)).thenReturn(reference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceSerializer.serialize(userReference)).thenReturn("user:Toto");
        when(this.userReferenceResolver.resolve("user:Toto")).thenReturn(userReference);

        String filterQuery = "filter(rankedElement:wiki\\:foobar) AND filter(entityType:PAGE) "
            + "AND filter(voter:user\\:Toto) AND filter(managerId:saveRank3)";
        SolrQuery expectedQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .addSort("createdAt", SolrQuery.ORDER.asc);
        QueryResponse response = prepareSolrClientQueryWhenStatement(this.solrClient, expectedQuery);

        // We don't mock stream behaviour, so that the returned result is empty.
        when(response.getResults()).thenReturn(this.documentList);

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("id", "myRanking");
        fieldMap.put(RankingQueryField.VOTE.getFieldName(), oldVote);
        fieldMap.put(RankingQueryField.CREATED_DATE.getFieldName(), new Date(422));
        fieldMap.put(RankingQueryField.UPDATED_DATE.getFieldName(), new Date(422));
        fieldMap.put(RankingQueryField.USER_REFERENCE.getFieldName(), "user:Toto");
        fieldMap.put(RankingQueryField.ENTITY_REFERENCE.getFieldName(), "wiki:foobar");
        fieldMap.put(RankingQueryField.ENTITY_TYPE.getFieldName(), "PAGE");
        fieldMap.put(RankingQueryField.SCALE.getFieldName(), scale);
        fieldMap.put(RankingQueryField.MANAGER_ID.getFieldName(), managerId);

        SolrDocument solrDocument = new SolrDocument(fieldMap);
        when(this.documentList.stream()).thenReturn(Collections.singletonList(solrDocument).stream());

        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RankingSolrCoreInitializer.DEFAULT_RANKING_SOLR_CORE)).thenReturn(this.solrClient);

        when(this.configuration.storeZero()).thenReturn(false);

        // Handle Average rank
        when(this.configuration.storeAverage()).thenReturn(true);
        SolrClient averageSolrClient = mock(SolrClient.class);
        when(this.solr.getClient(AverageRankSolrCoreInitializer.DEFAULT_AVERAGE_RANK_SOLR_CORE))
            .thenReturn(averageSolrClient);

        filterQuery = "filter(managerId:saveRank3) AND filter(rankedElement:wiki\\:foobar) "
            + "AND filter(entityType:PAGE)";
        SolrQuery averageQuery = new SolrQuery().addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .setSort("updatedAt", SolrQuery.ORDER.asc);
        QueryResponse averageResponse = prepareSolrClientQueryWhenStatement(averageSolrClient, averageQuery);
        SolrDocumentList averageDocumentList = mock(SolrDocumentList.class);
        when(averageResponse.getResults()).thenReturn(averageDocumentList);
        when(averageDocumentList.isEmpty()).thenReturn(false);

        fieldMap = new HashMap<>();
        fieldMap.put("id", "average1");
        fieldMap.put(AverageRankField.AVERAGE.getFieldName(), 6.0);
        fieldMap.put(AverageRankField.VOTE_NUMBER.getFieldName(), 2L);
        fieldMap.put(AverageRankField.RANKED_ELEMENT.getFieldName(), "wiki:foobar");
        fieldMap.put(AverageRankField.ENTITY_TYPE.getFieldName(), "PAGE");
        fieldMap.put(AverageRankField.MANAGER_ID.getFieldName(), managerId);
        fieldMap.put(AverageRankField.UPDATED_AT.getFieldName(), new Date(42));
        fieldMap.put(AverageRankField.SCALE.getFieldName(), scale);
        solrDocument = new SolrDocument(fieldMap);
        when(averageDocumentList.get(0)).thenReturn(solrDocument);

        DefaultAverageRank expectedModifiedAverage = new DefaultAverageRank("average1")
            .setAverageRank(5.5) // ((6 * 2) - (3 + 2)) / 2 -> 11 / 2 -> 5.5
            .setRankingNumber(2)
            .setManagerId(managerId)
            .setRankedElement(reference)
            .setScale(scale);
        SolrInputDocument expectedAverageInputDocument = new SolrInputDocument();
        expectedAverageInputDocument.setField("id", "average1");
        expectedAverageInputDocument.setField(AverageRankField.RANKED_ELEMENT.getFieldName(), "wiki:foobar");
        expectedAverageInputDocument.setField(AverageRankField.UPDATED_AT.getFieldName(), new Date(0));
        expectedAverageInputDocument.setField(AverageRankField.VOTE_NUMBER.getFieldName(), 2L);
        expectedAverageInputDocument.setField(AverageRankField.SCALE.getFieldName(), scale);
        expectedAverageInputDocument.setField(AverageRankField.MANAGER_ID.getFieldName(), managerId);
        expectedAverageInputDocument.setField(AverageRankField.AVERAGE.getFieldName(), 5.5);
        expectedAverageInputDocument.setField(AverageRankField.ENTITY_TYPE.getFieldName(), "PAGE");

        when(averageSolrClient.add(any(SolrInputDocument.class))).then(invocationOnMock -> {
            SolrInputDocument obtainedInputDocument = invocationOnMock.getArgument(0);
            Date updatedAt = (Date) obtainedInputDocument.getFieldValue("updatedAt");
            expectedAverageInputDocument.setField(AverageRankField.UPDATED_AT.getFieldName(), updatedAt);
            expectedModifiedAverage.setUpdatedAt(updatedAt);
            // We rely on the toString method since there's no proper equals method
            assertEquals(expectedAverageInputDocument.toString(), obtainedInputDocument.toString());
            return null;
        });

        DefaultRanking expectedRanking = new DefaultRanking("myRanking")
            .setManagerId(managerId)
            .setRankedElement(reference)
            .setCreatedAt(new Date(422))
            .setUpdatedAt(new Date())
            .setRank(newVote)
            .setScale(scale)
            .setVoter(userReference);

        SolrInputDocument expectedInputDocument = new SolrInputDocument();
        expectedInputDocument.setField("id", "myRanking");
        expectedInputDocument.setField(RankingQueryField.ENTITY_REFERENCE.getFieldName(), "wiki:foobar");
        expectedInputDocument.setField(RankingQueryField.ENTITY_TYPE.getFieldName(), "PAGE");
        expectedInputDocument.setField(RankingQueryField.CREATED_DATE.getFieldName(), new Date(422));
        expectedInputDocument.setField(RankingQueryField.UPDATED_DATE.getFieldName(), new Date());
        expectedInputDocument.setField(RankingQueryField.USER_REFERENCE.getFieldName(), "user:Toto");
        expectedInputDocument.setField(RankingQueryField.SCALE.getFieldName(), scale);
        expectedInputDocument.setField(RankingQueryField.MANAGER_ID.getFieldName(), managerId);
        expectedInputDocument.setField(RankingQueryField.VOTE.getFieldName(), newVote);

        when(this.solrClient.add(any(SolrInputDocument.class))).then(invocationOnMock -> {
            SolrInputDocument obtainedInputDocument = invocationOnMock.getArgument(0);
            Date updatedAt = (Date) obtainedInputDocument.getFieldValue("updatedAt");
            expectedInputDocument.setField(RankingQueryField.UPDATED_DATE.getFieldName(), updatedAt);

            expectedRanking
                .setUpdatedAt(updatedAt);
            // We rely on the toString method since there's no proper equals method
            assertEquals(expectedInputDocument.toString(), obtainedInputDocument.toString());
            return null;
        });

        assertEquals(expectedRanking, this.manager.saveRank(reference, userReference, newVote));
        verify(this.solrClient).add(any(SolrInputDocument.class));
        verify(this.solrClient).commit();
        verify(this.observationManager).notify(new UpdatedRankingEvent(oldVote), managerId, expectedRanking);
        verify(averageSolrClient).add(any(SolrInputDocument.class));
        verify(averageSolrClient).commit();
        verify(this.observationManager)
            .notify(any(UpdatedAverageRankEvent.class), eq(managerId), eq(expectedModifiedAverage));
    }

    @Test
    void saveRankExistingToZero() throws Exception
    {
        String managerId = "saveRank4";
        this.manager.setIdentifer(managerId);
        int scale = 8;
        int newVote = 0;
        int oldVote = 3;
        when(this.configuration.getScale()).thenReturn(scale);
        EntityReference reference = new EntityReference("foobar", EntityType.PAGE);
        when(this.entityReferenceSerializer.serialize(reference)).thenReturn("wiki:foobar");
        when(this.entityReferenceResolver.resolve("wiki:foobar", EntityType.PAGE)).thenReturn(reference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceSerializer.serialize(userReference)).thenReturn("user:Toto");
        when(this.userReferenceResolver.resolve("user:Toto")).thenReturn(userReference);

        String filterQuery = "filter(rankedElement:wiki\\:foobar) AND filter(entityType:PAGE) "
            + "AND filter(voter:user\\:Toto) AND filter(managerId:saveRank4)";
        SolrQuery firstExpectedQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .addSort("createdAt", SolrQuery.ORDER.asc);

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("id", "myRanking");
        fieldMap.put(RankingQueryField.VOTE.getFieldName(), oldVote);
        fieldMap.put(RankingQueryField.CREATED_DATE.getFieldName(), new Date(422));
        fieldMap.put(RankingQueryField.UPDATED_DATE.getFieldName(), new Date(422));
        fieldMap.put(RankingQueryField.USER_REFERENCE.getFieldName(), "user:Toto");
        fieldMap.put(RankingQueryField.ENTITY_REFERENCE.getFieldName(), "wiki:foobar");
        fieldMap.put(RankingQueryField.ENTITY_TYPE.getFieldName(), "PAGE");
        fieldMap.put(RankingQueryField.SCALE.getFieldName(), scale);
        fieldMap.put(RankingQueryField.MANAGER_ID.getFieldName(), managerId);

        SolrDocument solrDocument = new SolrDocument(fieldMap);
        when(this.documentList.stream())
            .thenReturn(Collections.singletonList(solrDocument).stream())
            .thenReturn(Collections.singletonList(solrDocument).stream());
        // Those are used for deletion.
        when(this.documentList.isEmpty()).thenReturn(false);
        when(this.documentList.get(0)).thenReturn(solrDocument);

        when(this.configuration.hasDedicatedCore()).thenReturn(false);
        when(this.solr.getClient(RankingSolrCoreInitializer.DEFAULT_RANKING_SOLR_CORE)).thenReturn(this.solrClient);

        // Handle Average rank
        when(this.configuration.storeAverage()).thenReturn(true);
        SolrClient averageSolrClient = mock(SolrClient.class);
        when(this.solr.getClient(AverageRankSolrCoreInitializer.DEFAULT_AVERAGE_RANK_SOLR_CORE))
            .thenReturn(averageSolrClient);

        filterQuery = "filter(managerId:saveRank4) AND filter(rankedElement:wiki\\:foobar) "
            + "AND filter(entityType:PAGE)";
        SolrQuery averageQuery = new SolrQuery().addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .setSort("updatedAt", SolrQuery.ORDER.asc);
        QueryResponse averageResponse = prepareSolrClientQueryWhenStatement(averageSolrClient, averageQuery);
        SolrDocumentList averageDocumentList = mock(SolrDocumentList.class);
        when(averageResponse.getResults()).thenReturn(averageDocumentList);
        when(averageDocumentList.isEmpty()).thenReturn(false);

        fieldMap = new HashMap<>();
        fieldMap.put("id", "average1");
        fieldMap.put(AverageRankField.AVERAGE.getFieldName(), 6.0);
        fieldMap.put(AverageRankField.VOTE_NUMBER.getFieldName(), 2L);
        fieldMap.put(AverageRankField.RANKED_ELEMENT.getFieldName(), "wiki:foobar");
        fieldMap.put(AverageRankField.ENTITY_TYPE.getFieldName(), "PAGE");
        fieldMap.put(AverageRankField.MANAGER_ID.getFieldName(), managerId);
        fieldMap.put(AverageRankField.UPDATED_AT.getFieldName(), new Date(42));
        fieldMap.put(AverageRankField.SCALE.getFieldName(), scale);
        solrDocument = new SolrDocument(fieldMap);
        when(averageDocumentList.get(0)).thenReturn(solrDocument);

        DefaultAverageRank expectedModifiedAverage = new DefaultAverageRank("average1")
            .setAverageRank(9) // ((6 * 2) - 3) / 1 -> 9
            .setRankingNumber(1)
            .setManagerId(managerId)
            .setRankedElement(reference)
            .setScale(scale);
        SolrInputDocument expectedAverageInputDocument = new SolrInputDocument();
        expectedAverageInputDocument.setField("id", "average1");
        expectedAverageInputDocument.setField(AverageRankField.RANKED_ELEMENT.getFieldName(), "wiki:foobar");
        expectedAverageInputDocument.setField(AverageRankField.UPDATED_AT.getFieldName(), new Date(0));
        expectedAverageInputDocument.setField(AverageRankField.VOTE_NUMBER.getFieldName(), 1L);
        expectedAverageInputDocument.setField(AverageRankField.SCALE.getFieldName(), scale);
        expectedAverageInputDocument.setField(AverageRankField.MANAGER_ID.getFieldName(), managerId);
        expectedAverageInputDocument.setField(AverageRankField.AVERAGE.getFieldName(), 9.0);
        expectedAverageInputDocument.setField(AverageRankField.ENTITY_TYPE.getFieldName(), "PAGE");

        when(averageSolrClient.add(any(SolrInputDocument.class))).then(invocationOnMock -> {
            SolrInputDocument obtainedInputDocument = invocationOnMock.getArgument(0);
            Date updatedAt = (Date) obtainedInputDocument.getFieldValue("updatedAt");
            expectedAverageInputDocument.setField(AverageRankField.UPDATED_AT.getFieldName(), updatedAt);
            expectedModifiedAverage.setUpdatedAt(updatedAt);
            // We rely on the toString method since there's no proper equals method
            assertEquals(expectedAverageInputDocument.toString(), obtainedInputDocument.toString());
            return null;
        });

        DefaultRanking oldRanking = new DefaultRanking("myRanking")
            .setManagerId(managerId)
            .setRankedElement(reference)
            .setCreatedAt(new Date(422))
            .setUpdatedAt(new Date(422))
            .setRank(oldVote)
            .setScale(scale)
            .setVoter(userReference);

        filterQuery = "filter(id:myRanking) AND filter(managerId:saveRank4)";
        SolrQuery secondExpectedQuery = new SolrQuery()
            .addFilterQuery(filterQuery)
            .setStart(0)
            .setRows(1)
            .addSort("createdAt", SolrQuery.ORDER.asc);

        QueryResponse response = mock(QueryResponse.class);
        final AtomicBoolean flag = new AtomicBoolean(false);
        when(solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery givenQuery = invocationOnMock.getArgument(0);
            SolrQuery checkExpectedQuery;
            if (!flag.get()) {
                checkExpectedQuery = firstExpectedQuery;
                flag.set(true);
            } else {
                checkExpectedQuery = secondExpectedQuery;
            }

            assertEquals(checkExpectedQuery.getQuery(), givenQuery.getQuery());
            assertArrayEquals(checkExpectedQuery.getFilterQueries(), givenQuery.getFilterQueries());
            assertEquals(checkExpectedQuery.getRows(), givenQuery.getRows());
            assertEquals(checkExpectedQuery.getStart(), givenQuery.getStart());
            assertEquals(checkExpectedQuery.getSorts(), givenQuery.getSorts());
            return response;
        });
        when(response.getResults()).thenReturn(this.documentList);

        when(this.configuration.storeZero()).thenReturn(true);
        assertNull(this.manager.saveRank(reference, userReference, newVote));
        verify(this.solrClient, never()).add(any(SolrInputDocument.class));
        verify(this.solrClient).deleteById("myRanking");
        verify(this.solrClient).commit();
        verify(this.observationManager).notify(any(DeletedRankingEvent.class), eq(managerId), eq(oldRanking));
        verify(averageSolrClient).add(any(SolrInputDocument.class));
        verify(averageSolrClient).commit();
        verify(this.observationManager)
            .notify(any(UpdatedAverageRankEvent.class), eq(managerId), eq(expectedModifiedAverage));
    }
}
