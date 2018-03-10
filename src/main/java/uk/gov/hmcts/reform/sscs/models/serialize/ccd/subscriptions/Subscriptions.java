package uk.gov.hmcts.reform.sscs.models.serialize.ccd.subscriptions;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Subscriptions {
    Subscription appellantSubscription;
    Subscription supporterSubscription;
}
