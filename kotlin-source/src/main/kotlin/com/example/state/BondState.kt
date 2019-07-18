package com.example.state

import com.example.schema.BondSchema
import com.example.schema.BondSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*

data class BondState(
                    val issuer: Party,
                    val borrower: Party,
                    val bondName: String,
                    val duration: Int,
                    val total: Int,
                    val amount: Int,
                    val unit: Int,
                    val issueDate: Date,
                    val maturityDate: Date,
                    val interestRate: Double,
                    override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(issuer, borrower)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is BondSchemaV1 -> BondSchemaV1.PersistentBond(
                    this.issuer.name.toString(),
                    this.borrower.name.toString(),
                    this.bondName,
                    this.duration,
                    this.total,
                    this.amount,
                    this.unit,
                    this.issueDate.time.toString(),
                    this.maturityDate.time.toString(),
                    this.interestRate,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BondSchemaV1)
}
