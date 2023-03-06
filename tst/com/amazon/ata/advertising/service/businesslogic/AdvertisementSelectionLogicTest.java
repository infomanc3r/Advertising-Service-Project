package com.amazon.ata.advertising.service.businesslogic;

import com.amazon.ata.advertising.service.dao.ReadableDao;
import com.amazon.ata.advertising.service.model.AdvertisementContent;
import com.amazon.ata.advertising.service.model.EmptyGeneratedAdvertisement;
import com.amazon.ata.advertising.service.model.GeneratedAdvertisement;
import com.amazon.ata.advertising.service.targeting.TargetingEvaluator;
import com.amazon.ata.advertising.service.targeting.TargetingGroup;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AdvertisementSelectionLogicTest {

    private static final String CUSTOMER_ID = "A123B456";
    private static final String MARKETPLACE_ID = "1";

    private static final String CONTENT_ID1 = UUID.randomUUID().toString();
    private static final AdvertisementContent CONTENT1 = AdvertisementContent.builder().withContentId(CONTENT_ID1).build();
    private static final String CONTENT_ID2 = UUID.randomUUID().toString();
    private static final AdvertisementContent CONTENT2 = AdvertisementContent.builder().withContentId(CONTENT_ID2).build();
    private static final String CONTENT_ID3 = UUID.randomUUID().toString();
    private static final AdvertisementContent CONTENT3 = AdvertisementContent.builder().withContentId(CONTENT_ID3).build();
    private static final String CONTENT_ID4 = UUID.randomUUID().toString();
    private static final AdvertisementContent CONTENT4 = AdvertisementContent.builder().withContentId(CONTENT_ID4).build();

    @Mock
    private ReadableDao<String, List<AdvertisementContent>> contentDao;

    @Mock
    private ReadableDao<String, List<TargetingGroup>> targetingGroupDao;

    @Mock
    private Random random;

    private AdvertisementSelectionLogic adSelectionService;

    @Mock
    private TargetingEvaluator targetingEvaluator;


    @BeforeEach
    public void setup() {
        initMocks(this);
        adSelectionService = new AdvertisementSelectionLogic(contentDao, targetingGroupDao);
        adSelectionService.setRandom(random);
    }

    @Test
    public void selectAdvertisement_nullMarketplaceId_EmptyAdReturned() {
        GeneratedAdvertisement ad = adSelectionService.selectAdvertisement(CUSTOMER_ID, null);
        assertTrue(ad instanceof EmptyGeneratedAdvertisement);
    }

    @Test
    public void selectAdvertisement_emptyMarketplaceId_EmptyAdReturned() {
        GeneratedAdvertisement ad = adSelectionService.selectAdvertisement(CUSTOMER_ID, "");
        assertTrue(ad instanceof EmptyGeneratedAdvertisement);
    }

    @Test
    public void selectAdvertisement_noContentForMarketplace_emptyAdReturned() throws InterruptedException {
        when(contentDao.get(MARKETPLACE_ID)).thenReturn(Collections.emptyList());

        GeneratedAdvertisement ad = adSelectionService.selectAdvertisement(CUSTOMER_ID, MARKETPLACE_ID);

        assertTrue(ad instanceof EmptyGeneratedAdvertisement);
    }


    @Test
    public void selectAdvertisement_oneAd_returnsAd() {
        List<AdvertisementContent> contents = Arrays.asList(CONTENT1);
        List<TargetingGroup> targetingGroupList = new ArrayList<>();
        TargetingGroup targetingGroup = new TargetingGroup();
        targetingGroup.setTargetingPredicates(new ArrayList<>());
        targetingGroup.setContentId(CONTENT_ID1);
        targetingGroup.setClickThroughRate(.5);
        targetingGroupList.add(targetingGroup);

        when(contentDao.get(MARKETPLACE_ID)).thenReturn(contents);
        when(random.nextInt(contents.size())).thenReturn(0);
        when(targetingGroupDao.get(CONTENT_ID1)).thenReturn(targetingGroupList);
        when(targetingEvaluator.evaluate(any(TargetingGroup.class))).thenReturn(TargetingPredicateResult.TRUE);

        GeneratedAdvertisement ad = adSelectionService.selectAdvertisement(CUSTOMER_ID, MARKETPLACE_ID);

        assertEquals(CONTENT_ID1, ad.getContent().getContentId());
    }

    @Test
    public void selectAdvertisement_multipleAds_returnsHighestClickthroughRate() {
        List<AdvertisementContent> contents = Arrays.asList(CONTENT1, CONTENT2, CONTENT3);
        List<TargetingGroup> targetingGroupList1 = new ArrayList<>();
        TargetingGroup targetingGroup1 = new TargetingGroup();
        targetingGroup1.setClickThroughRate(.1);
        targetingGroup1.setContentId(CONTENT_ID1);
        targetingGroup1.setTargetingPredicates(new ArrayList<>());
        targetingGroupList1.add(targetingGroup1);
        List<TargetingGroup> targetingGroupList2 = new ArrayList<>();
        TargetingGroup targetingGroup2 = new TargetingGroup();
        targetingGroup2.setClickThroughRate(.5);
        targetingGroup2.setContentId(CONTENT_ID2);
        targetingGroup2.setTargetingPredicates(new ArrayList<>());
        targetingGroupList2.add(targetingGroup2);
        List<TargetingGroup> targetingGroupList3 = new ArrayList<>();
        TargetingGroup targetingGroup3 = new TargetingGroup();
        targetingGroup3.setClickThroughRate(.2);
        targetingGroup3.setContentId(CONTENT_ID3);
        targetingGroup3.setTargetingPredicates(new ArrayList<>());
        targetingGroupList3.add(targetingGroup3);

        when(contentDao.get(MARKETPLACE_ID)).thenReturn(contents);
        when(random.nextInt(contents.size())).thenReturn(1);
        when(targetingGroupDao.get(CONTENT_ID1)).thenReturn(targetingGroupList1);
        when(targetingGroupDao.get(CONTENT_ID2)).thenReturn(targetingGroupList2);
        when(targetingGroupDao.get(CONTENT_ID3)).thenReturn(targetingGroupList3);
        when(targetingEvaluator.evaluate(any(TargetingGroup.class))).thenReturn(TargetingPredicateResult.TRUE);

        GeneratedAdvertisement ad = adSelectionService.selectAdvertisement(CUSTOMER_ID, MARKETPLACE_ID);

        assertEquals(CONTENT_ID2, ad.getContent().getContentId());
    }

}
