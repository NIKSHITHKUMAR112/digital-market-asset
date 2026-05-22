package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketplaceDao {
    // Profile
    @Query("SELECT * FROM profiles WHERE id = :id")
    fun getProfileFlow(id: String = "current_user"): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileDirect(id: String = "current_user"): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    // Products
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE category = :category ORDER BY id DESC")
    fun getProductsByCategoryFlow(category: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Query("UPDATE products SET status = :status, ownerWallet = :ownerWallet WHERE id = :id")
    suspend fun updateProductOwnership(id: Long, status: String, ownerWallet: String)

    @Query("UPDATE products SET isWishlisted = :isWishlisted WHERE id = :id")
    suspend fun updateProductWishlist(id: Long, isWishlisted: Boolean)

    @Query("UPDATE products SET isFlagged = :isFlagged WHERE id = :id")
    suspend fun updateProductFlag(id: Long, isFlagged: Boolean)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // Chat
    @Query("SELECT * FROM chat_messages WHERE productId = :productId ORDER BY timestamp ASC")
    fun getMessagesForProductFlow(productId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)
}

@Database(
    entities = [ProfileEntity::class, ProductEntity::class, TransactionEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun marketplaceDao(): MarketplaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "market_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MarketplaceRepository(private val dao: MarketplaceDao) {
    val currentProfile: Flow<ProfileEntity?> = dao.getProfileFlow()
    val allProducts: Flow<List<ProductEntity>> = dao.getAllProductsFlow()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactionsFlow()

    suspend fun getProfile(): ProfileEntity? = dao.getProfileDirect()
    suspend fun saveProfile(profile: ProfileEntity) = dao.insertProfile(profile)

    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> = dao.getProductsByCategoryFlow(category)
    suspend fun getProductById(id: Long): ProductEntity? = dao.getProductById(id)
    suspend fun addProduct(product: ProductEntity): Long = dao.insertProduct(product)
    
    suspend fun buyProduct(productId: Long, buyerWallet: String, newOwnerWallet: String, priceEth: Double, txHash: String) {
        dao.updateProductOwnership(productId, "SOLD", newOwnerWallet)
        
        // Record purchase transaction
        dao.insertTransaction(
            TransactionEntity(
                type = "PURCHASE",
                productTitle = (dao.getProductById(productId)?.title ?: "Asset"),
                amountEth = priceEth,
                txHash = txHash,
                senderWallet = buyerWallet,
                receiverWallet = newOwnerWallet,
                status = "SUCCESS",
                gasPaidEth = 0.0035,
                blockNumber = 18492045
            )
        )
        
        // Decrease profile eth balance
        val profile = dao.getProfileDirect()
        if (profile != null && profile.walletAddress == buyerWallet) {
            val updatedProfile = profile.copy(ethBalance = maxOf(0.0, profile.ethBalance - priceEth - 0.0035))
            dao.insertProfile(updatedProfile)
        }
    }

    suspend fun toggleWishlist(id: Long, isWishlisted: Boolean) {
        dao.updateProductWishlist(id, isWishlisted)
    }

    suspend fun flagProduct(id: Long, isFlagged: Boolean) {
        dao.updateProductFlag(id, isFlagged)
    }

    suspend fun addTransaction(transaction: TransactionEntity) = dao.insertTransaction(transaction)

    fun getChatMessages(productId: Long): Flow<List<ChatMessageEntity>> = dao.getMessagesForProductFlow(productId)
    suspend fun sendChatMessage(message: ChatMessageEntity) = dao.insertChatMessage(message)

    // Prepopulate initial data helper
    suspend fun prepopulateDataIfNeeded() {
        val existingProfile = getProfile()
        if (existingProfile == null) {
            val starterProfile = ProfileEntity(
                fullName = "Kaelen Vex",
                username = "cyber_vex_99",
                email = "kaelen@dma.network",
                phoneNumber = "+1 (555) 0192-NEON",
                walletAddress = "0x7F2e...A4e9",
                profileImage = "cyber_avatar_1",
                kycStatus = "VERIFIED",
                ethBalance = 5.240
            )
            saveProfile(starterProfile)

            // Let's add some default products
            val items = listOf(
                ProductEntity(
                    title = "Aetherius Cyber Ape #8490",
                    description = "An ultra-rare cyberpunk primate infused with nanotech shaders and an illuminated matrix visor. Minted live on Ethereum L2, verified by decentralized quantum contract.",
                    priceEth = 1.45,
                    priceUsd = 4310.0,
                    imageUrl = "nft_ape",
                    category = "NFT",
                    creatorWallet = "0x3D11...8C42",
                    creatorName = "Neo_Minter",
                    ownerWallet = "0x3D11...8C42",
                    smartContractAddress = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D",
                    tokenId = "8490",
                    status = "AVAILABLE",
                    likesCount = 88,
                    txHash = "0x9c4883ab0b938fd37c569f1437cc330f8d9b626781fbc0dc2d9ce6e98b045f91",
                    ipfsHash = "ipfs://QmXGTzSLeXyTjDsb2R6LzE5K7YVzH2D9sF4pTx8NnC7X2U"
                ),
                ProductEntity(
                    title = "Metaverse CyberPenthouse #33",
                    description = "Premium real estate in Core Sector 7. Panoramic holograms of the main neon grid, infinite dynamic lighting control, and digital asset display integrations.",
                    priceEth = 2.80,
                    priceUsd = 8320.0,
                    imageUrl = "mv_penthouse",
                    category = "Metaverse",
                    creatorWallet = "0x8E19...2B45",
                    creatorName = "NetArch_Kole",
                    ownerWallet = "0x8E19...2B45",
                    smartContractAddress = "0xD41B4CFF6D002B45AC488D002B4CFF6D002B4CEE",
                    tokenId = "33",
                    status = "AVAILABLE",
                    likesCount = 142,
                    txHash = "0x12a883ab0b938fd37c569f1437cc330f8d9b626781fbc0dc2d9ce6e98b045e31",
                    ipfsHash = "ipfs://QmTpdSLeXyTjDsb2R6LzE5K7YVzH2D9sF4pTx8NnC9W3A"
                ),
                ProductEntity(
                    title = "Xanthar Quantum Edge Laser Blade",
                    description = "In-game wearable item for Xanthar Online. High energy containment field, 1500+ dps thermal discharge, registered under ENJ-22 smart registry protocol.",
                    priceEth = 0.22,
                    priceUsd = 650.0,
                    imageUrl = "wearable_blade",
                    category = "Game Item",
                    creatorWallet = "0x5E81...9F01",
                    creatorName = "XantharLabs",
                    ownerWallet = "0x5E81...9F01",
                    smartContractAddress = "0xE12A4CFF6D002B45AC488D002B4CFF6D002B4A11",
                    tokenId = "4155",
                    status = "AVAILABLE",
                    likesCount = 34,
                    txHash = "0xbf4883ab0b938fd37c569f1437cc330f8d9b626781fbc0dc2d9ce6e98b045f09",
                    ipfsHash = "ipfs://QmY781LeXyTjDsb2R6LzE5K7YVzH2D9sF4pTx8NnC3L2B"
                ),
                ProductEntity(
                    title = "Digital Matrix Abstract III",
                    description = "A generative audiovisual algorithmic masterpiece that dynamically responds to fluctuating Ethereum gas prices, showing real-time ambient noise.",
                    priceEth = 0.75,
                    priceUsd = 2230.0,
                    imageUrl = "art_abstract",
                    category = "Digital Art",
                    creatorWallet = "0xAA2B...BB91",
                    creatorName = "Sol_Analytica",
                    ownerWallet = "0xAA2B...BB91",
                    smartContractAddress = "0xF7150d5630B4cF539739dF2C5dAcb4c659F2412B",
                    tokenId = "3",
                    status = "AVAILABLE",
                    likesCount = 59,
                    txHash = "0x0a1223ab0b938fd37c569f1437cc330f8d9b626781fbc0dc2d9ce6e98b045fa4",
                    ipfsHash = "ipfs://QmS88ZLeXyTjDsb2R6LzE5K7YVzH2D9sF4pTx8NnC3P2A"
                ),
                ProductEntity(
                    title = "Cyberwear Haptic Matrix Jacket",
                    description = "Limited edition PHYSICAL high-collar jacket combined with direct NFT claim link. High-output fiber-optics woven in fabric sync with your crypto wallet activity.",
                    priceEth = 1.10,
                    priceUsd = 3270.0,
                    imageUrl = "phys_jacket",
                    category = "Physical",
                    creatorWallet = "0xEF34...73AA",
                    creatorName = "HapticGrid",
                    ownerWallet = "0xEF34...73AA",
                    smartContractAddress = "0x89220d5630B4cF539739dF2C5dAcb4c659F2477C",
                    tokenId = "120",
                    status = "AVAILABLE",
                    likesCount = 205,
                    txHash = "0xa34883ab0b938fd37c569f1437cc330f8d9b626781fbc0dc2d9ce6e98b045fde",
                    ipfsHash = "ipfs://QmPL1ZLeXyTjDsb2R6LzE5K7YVzH2D9sF4pTx8NnC8T2Q"
                ),
                ProductEntity(
                    title = "Genesis Quantum Node Keycard #001",
                    description = "An ultra-secure, elite digital keycard granting priority validation bandwidth, high escrow APY yields, and executive smart matrix voting rights.",
                    priceEth = 0.50,
                    priceUsd = 1560.0,
                    imageUrl = "nft_keycard",
                    category = "NFT",
                    creatorWallet = "0x1A2B...99DD",
                    creatorName = "Founder_V0",
                    ownerWallet = "0x1A2B...99DD",
                    smartContractAddress = "0x51E20d5630B4cF539739dF2C5dAcb4c659F2412A",
                    tokenId = "1",
                    status = "AVAILABLE",
                    likesCount = 312,
                    txHash = "0x111223ab0b938fd37c569f1437cc330f8d9b626781fbc0dc2d9ce6e98b045f20",
                    ipfsHash = "ipfs://QmQS88ZLeXyTjDsb2R6LzE5K7YVzH2D9sF4pTx8NnC3P2A"
                )
            )
            items.forEach { addProduct(it) }

            // Add an initial greeting message from the Cyber Assistant
            sendChatMessage(
                ChatMessageEntity(
                    productId = 0,
                    senderName = "DMA AI Sentinel",
                    senderAddress = "0x0000...0000",
                    text = "System initialized. Welcome to the Digital Market Asset matrix. I am your AI Smart Sentinel. I can analyze any listed smart contracts for vulnerabilities, estimate current network gas metrics, check KYC integrity, or recommend top trend assets. What security queries do you have regarding Web3 assets?",
                    isAiAssistant = true
                )
            )
        }
    }
}
