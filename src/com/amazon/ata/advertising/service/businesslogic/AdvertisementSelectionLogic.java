package com.amazon.ata.advertising.service.businesslogic;

import com.amazon.ata.advertising.service.dao.ReadableDao;
import com.amazon.ata.advertising.service.model.*;
import com.amazon.ata.advertising.service.targeting.TargetingEvaluator;
import com.amazon.ata.advertising.service.targeting.TargetingGroup;

import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * This class is responsible for picking the advertisement to be rendered.
 */
public class AdvertisementSelectionLogic {

    private static final Logger LOG = LogManager.getLogger(AdvertisementSelectionLogic.class);

    private final ReadableDao<String, List<AdvertisementContent>> contentDao;
    private final ReadableDao<String, List<TargetingGroup>> targetingGroupDao;
    private Random random = new Random();

    /**
     * Constructor for AdvertisementSelectionLogic.
     * @param contentDao Source of advertising content.
     * @param targetingGroupDao Source of targeting groups for each advertising content.
     */
    @Inject
    public AdvertisementSelectionLogic(ReadableDao<String, List<AdvertisementContent>> contentDao,
                                       ReadableDao<String, List<TargetingGroup>> targetingGroupDao) {
        this.contentDao = contentDao;
        this.targetingGroupDao = targetingGroupDao;
    }

    /**
     * Setter for Random class.
     * @param random generates random number used to select advertisements.
     */
    public void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Gets all of the content and metadata for the marketplace and determines which content can be shown.  Returns the
     * eligible content with the highest click through rate.  If no advertisement is available or eligible, returns an
     * EmptyGeneratedAdvertisement.
     *
     * @param customerId - the customer to generate a custom advertisement for
     * @param marketplaceId - the id of the marketplace the advertisement will be rendered on
     * @return an advertisement customized for the customer id provided, or an empty advertisement if one could
     *     not be generated.
     */
    public GeneratedAdvertisement selectAdvertisement(String customerId, String marketplaceId) {

        GeneratedAdvertisement selectedContent = new EmptyGeneratedAdvertisement();

        RequestContext requestContext = new RequestContext(customerId, marketplaceId);

        TargetingEvaluator targetingEvaluator = new TargetingEvaluator(requestContext);

        final List<AdvertisementContent> ads = contentDao.get(marketplaceId);

        if (marketplaceId == null || marketplaceId.equals("")) {
            return selectedContent;
        }

//        if (ads.size() == 1) {
//            return new GeneratedAdvertisement(ads.get(0));
//        } else if (ads.size() > 1) {
//            return new GeneratedAdvertisement(ads.get(random.nextInt(ads.size())));
//        }

        List<AdvertisementContent> filteredContent = ads.stream()
                .filter(targetingGroupList ->
                        targetingGroupDao.get(targetingGroupList.getContentId())
                        .stream()
                        .map(targetingEvaluator::evaluate)
                        .anyMatch(TargetingPredicateResult::isTrue))
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(filteredContent)) {
                AdvertisementContent randomAdvertisementContent =
                        filteredContent.get(random.nextInt(filteredContent.size()));
                selectedContent = new GeneratedAdvertisement(randomAdvertisementContent);
            }

        return selectedContent;

//        for (AdvertisementContent ad : ads) {
//
//            List<TargetingGroup> targetingGroupList = targetingGroupDao.get(ad.getContentId());
//
//            for (TargetingGroup targetingGroup : targetingGroupList) {
//                if (targetingEvaluator.evaluate(targetingGroup).isTrue()) {
//                    acceptableAds.add(ad);
//                }
//            }
//
//        }

//        GeneratedAdvertisement generatedAdvertisement = new EmptyGeneratedAdvertisement();
//        if (StringUtils.isEmpty(marketplaceId)) {
//            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
//        } else {
//            final List<AdvertisementContent> contents = contentDao.get(marketplaceId);
//
//            if (CollectionUtils.isNotEmpty(contents)) {
//                AdvertisementContent randomAdvertisementContent = contents.get(random.nextInt(contents.size()));
//                generatedAdvertisement = new GeneratedAdvertisement(randomAdvertisementContent);
//            }
//
//        }
//
//        return generatedAdvertisement;






    }
}
