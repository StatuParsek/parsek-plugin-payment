package co.statu.rule.plugins.payment.route.billdetail

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.error.PageNotFound
import co.statu.parsek.model.Path
import co.statu.parsek.model.Result
import co.statu.parsek.model.RouteType
import co.statu.parsek.model.Successful
import co.statu.rule.auth.api.LoggedInApi
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.DatabaseManager
import co.statu.rule.plugins.payment.PaymentPlugin
import co.statu.rule.plugins.payment.db.dao.BillDetailDao
import co.statu.rule.plugins.payment.db.impl.BillDetailDaoImpl
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas

@Endpoint
class GetBillDetailsAPI(
    private val paymentPlugin: PaymentPlugin
) : LoggedInApi() {
    private val authProvider by lazy {
        paymentPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        paymentPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    override val paths = listOf(Path("/billDetails", RouteType.GET))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .queryParameter(Parameters.optionalParam("page", Schemas.numberSchema()))
            .build()

    private val billDetailDao: BillDetailDao = BillDetailDaoImpl()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)

        val page = parameters.queryParameter("page")?.long ?: 1

        val userId = authProvider.getUserIdFromRoutingContext(context)

        val jdbcPool = databaseManager.getConnectionPool()

        val count = billDetailDao.countByUserId(userId, jdbcPool)

        var totalPage = kotlin.math.ceil(count.toDouble() / 10).toLong()

        if (totalPage < 1) {
            totalPage = 1
        }

        if (page > totalPage || page < 1) {
            throw PageNotFound()
        }

        val billDetails = billDetailDao.byUserId(userId, page, jdbcPool)

        return Successful(
            billDetails,
            mapOf(
                "totalCount" to count,
                "totalPage" to totalPage
            )
        )
    }
}