package ch.puzzle.itc.mobiliar.business.softlinkRelation.control;

import ch.puzzle.itc.mobiliar.builders.ResourceEntityBuilder;
import ch.puzzle.itc.mobiliar.business.auditview.control.AuditService;
import ch.puzzle.itc.mobiliar.business.auditview.control.ThreadLocalUtil;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.entity.SoftlinkRelationEntity;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SoftLinkRelationServiceTest {

    @InjectMocks
    @Spy
    SoftlinkRelationService softlinkRelationService = spy(new SoftlinkRelationService());

    @Spy
    AuditService auditService = spy(new AuditService());

    @Mock
    private EntityManager entityManager;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        ThreadLocalUtil.destroy();
    }

    @Test
    public void shouldSetResourceIdInThreadLocal() {
        // given
        ResourceEntity resourceEntity = ResourceEntityBuilder.createResourceEntity("MyResource", 500);
        TypedQuery<SoftlinkRelationEntity> query = mock(TypedQuery.class);
        when(query.getResultList()).thenReturn(Collections.emptyList());
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(entityManager.createQuery(any(String.class), eq(SoftlinkRelationEntity.class))).thenReturn(query);

        // when
        softlinkRelationService.removeSoftlinkRelation(resourceEntity);

        // then
        assertThat("The resourceId Param must be stored as ThreadLocal variable for auditing (envers)",
                ThreadLocalUtil.getThreadVariable(ThreadLocalUtil.KEY_RESOURCE_ID), is(CoreMatchers.notNullValue()));
        int actualResourceTypeId = (int) ThreadLocalUtil.getThreadVariable(ThreadLocalUtil.KEY_RESOURCE_ID);
        assertThat(actualResourceTypeId, is(500));
    }

}
