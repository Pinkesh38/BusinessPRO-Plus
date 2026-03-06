package com.example.businessproplus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface PartyDao {
    @Insert
    suspend fun insertParty(party: Party)

    @Update
    suspend fun updateParty(party: Party)

    @Delete
    suspend fun deleteParty(party: Party)

    @Query("UPDATE party_table SET isDeleted = 1 WHERE id = :partyId")
    suspend fun softDeleteParty(partyId: Int)

    @Query("SELECT * FROM party_table WHERE isDeleted = 0 ORDER BY companyName ASC")
    suspend fun getAllParties(): List<Party>

    @Query("SELECT * FROM party_table WHERE companyName = :name AND isDeleted = 0 LIMIT 1")
    suspend fun getPartyByName(name: String): Party?

    @Query("SELECT * FROM party_table WHERE id = :id LIMIT 1")
    suspend fun getPartyById(id: Int): Party?
}