package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String = "current_user",
    val fullName: String,
    val username: String,
    val email: String,
    val phoneNumber: String,
    val walletAddress: String,
    val profileImage: String, // Can be avatar name or URL
    val kycStatus: String, // "PENDING", "VERIFIED", "UNVERIFIED"
    val isTwoFactorEnabled: Boolean = false,
    val ethBalance: Double = 3.52, // Starting simulated balance in ETH
    val followersCount: Int = 142,
    val followingCount: Int = 89,
    val sellerRating: Float = 4.8f,
    val totalReviewsCount: Int = 26
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val priceEth: Double,
    val priceUsd: Double,
    val imageUrl: String,
    val category: String, // "NFT", "Game Item", "Digital Art", "Metaverse", "Physical"
    val creatorWallet: String,
    val creatorName: String,
    val ownerWallet: String,
    val smartContractAddress: String,
    val tokenId: String,
    val status: String, // "AVAILABLE", "ESCROWED", "SOLD"
    val likesCount: Int = 0,
    val isWishlisted: Boolean = false,
    val ratingsCount: Int = 0,
    val averageRating: Float = 5.0f,
    val isFlagged: Boolean = false,
    val txHash: String = "",
    val gasFeeEth: Double = 0.0021,
    val blockNumber: Long = 18492001,
    val ipfsHash: String = ""
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "MINT", "PURCHASE", "ESCROW_RELEASE", "DEPOSIT"
    val productTitle: String,
    val amountEth: Double,
    val txHash: String,
    val senderWallet: String,
    val receiverWallet: String,
    val status: String, // "PENDING", "SUCCESS", "FAILED"
    val gasPaidEth: Double,
    val blockNumber: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long, // 0 for general / AI counselor
    val senderName: String,
    val senderAddress: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAiAssistant: Boolean = false
)
