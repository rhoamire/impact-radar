package com.impactradar;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.impactradar.dto.RelationshipEvidence;
import com.impactradar.service.RelationshipEvidenceService;

class RelationshipEvidenceServiceTest {

    private final RelationshipEvidenceService service =
            new RelationshipEvidenceService();

    @Test
    void detectsDirectionalEvidenceFromQaToPayment() {

        String payment = """
                The Payment Service implements /payments.
                It returns payment_intent_id and payment_status.
                """;

        String qa = """
                QA validates Payment Service behavior.
                Tests cover /payments,
                payment_intent_id,
                payment_status,
                and idempotency_key.
                """;

        RelationshipEvidence evidence =
                service.analyze(
                        "Payment Service Design",
                        payment,
                        "QA Test Plan",
                        qa
                );

        assertThat(evidence.sharedIdentifiers())
                .contains(
                        "/payments",
                        "payment_intent_id",
                        "payment_status"
                );

        assertThat(evidence.directionalSignal())
                .isEqualTo("TARGET_DEPENDS_ON_SOURCE");

        assertThat(evidence.suggestedRelationshipType())
                .isEqualTo("depends_on");

        assertThat(evidence.evidenceConfidence())
                .isGreaterThan(0.5);
    }
}