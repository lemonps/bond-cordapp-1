package com.example.api

import com.example.state.BondState
import kotlinx.html.currentTimeMillis
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.training.flow.BondIssueFlow
import org.slf4j.Logger
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
class ExampleApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<ExampleApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GET
    @Path("ious")
    @Produces(MediaType.APPLICATION_JSON)
    fun getIOUs() = rpcOps.vaultQueryBy<BondState>().states

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the lender and the borrower will be able to
     * see it when calling /api/example/ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
//    @PUT
//    @Path("create-iou")
//    fun createIOU(@QueryParam("iouValue") iouValue: Int, @QueryParam("partyName") partyName: CordaX500Name?): Response {
//        if (iouValue <= 0 ) {
//            return Response.status(BAD_REQUEST).entity("Query parameter 'iouValue' must be non-negative.\n").build()
//        }
//        if (partyName == null) {
//            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build()
//        }
//        val otherParty = rpcOps.wellKnownPartyFromX500Name(partyName) ?:
//                return Response.status(BAD_REQUEST).entity("Party named $partyName cannot be found.\n").build()
//
//        return try {
//            val signedTx = rpcOps.startTrackedFlow(::Initiator, iouValue, otherParty).returnValue.getOrThrow()
//            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()
//
//        } catch (ex: Throwable) {
//            logger.error(ex.message, ex)
//            Response.status(BAD_REQUEST).entity(ex.message!!).build()
//        }
//    }
//
//	/**
//     * Displays all IOU states that are created by Party.
//     */
//    @GET
//    @Path("my-ious")
//    @Produces(MediaType.APPLICATION_JSON)
//    fun myious(): Response {
//        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
//        val results = builder {
//                var partyType = IOUSchemaV1.PersistentIOU::lenderName.equal(rpcOps.nodeInfo().legalIdentities.first().name.toString())
//                val customCriteria = QueryCriteria.VaultCustomQueryCriteria(partyType)
//                val criteria = generalCriteria.and(customCriteria)
//                val results = rpcOps.vaultQueryBy<BondState>(criteria).states
//                return Response.ok(results).build()
//        }
//    }

    @GET
    @Path("agent-issue-bond")
    fun issueBond (@QueryParam(value = "Bond_Name") name: String,
                   @QueryParam(value = "Amount") amount: Int,
                   @QueryParam(value = "Price_Per_Unit") unit: Int,
                   @QueryParam(value = "Duration") duration: Int,
                   @QueryParam(value = "Interest_Rate") interest: Double):
                Response {
        val me = rpcOps.nodeInfo().legalIdentities.first()
        try {
            val bondState = BondState(me, me, name, duration,100000, amount, unit, interest, UniqueIdentifier())
            rpcOps.startFlow(::BondIssueFlow, bondState).returnValue.get()
            return Response.status(Response.Status.CREATED).entity("Issue Bond Successfully").build()

        } catch (e: Exception) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.message)
                    .build()
        }
    }






}