package co.statu.rule.plugins.payment.error

import co.statu.parsek.model.Error

class InvalidTaxAdministration(
    statusMessage: String = "",
    extras: Map<String, Any> = mapOf()
) : Error(400, statusMessage, extras)