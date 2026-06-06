package com.rushi.coinmaster.data.local.database

import androidx.room.TypeConverter
import com.rushi.coinmaster.data.local.model.AccountType
import com.rushi.coinmaster.data.local.model.BucketType
import com.rushi.coinmaster.data.local.model.TransactionType

class AppConverters {

    @TypeConverter
    fun fromAccountType(type: AccountType): String = type.name

    @TypeConverter
    fun toAccountType(name: String): AccountType = AccountType.valueOf(name)

    @TypeConverter
    fun fromBucketType(type: BucketType): String = type.name

    @TypeConverter
    fun toBucketType(name: String): BucketType = BucketType.valueOf(name)

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(name: String): TransactionType = TransactionType.valueOf(name)
}
