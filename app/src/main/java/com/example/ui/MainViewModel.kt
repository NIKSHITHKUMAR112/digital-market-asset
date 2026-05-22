package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiRetrofitClient
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface AuthState {
    object Unauthenticated : AuthState
    object Authenticating : AuthState
    data class Authenticated(val profile: ProfileEntity, val walletConnected: Boolean) : AuthState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MarketplaceRepository(database.marketplaceDao())

    // UI States
    val profileState: StateFlow<ProfileEntity?> = repository.currentProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val productsState: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsState: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Authentication & Wallet Connection state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isConnectingWallet = MutableStateFlow(false)
    val isConnectingWallet: StateFlow<Boolean> = _isConnectingWallet.asStateFlow()

    // Active Category Filter
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Detail Product Selection
    private val _selectedProduct = MutableStateFlow<ProductEntity?>(null)
    val selectedProduct: StateFlow<ProductEntity?> = _selectedProduct.asStateFlow()

    // Transaction Processing Screen state
    private val _transactionStatus = MutableStateFlow<String?>(null) // "ESTIMATING", "SIGNING", "BROADCASTING", "CONFIRMING", "SUCCESS", null
    val transactionStatus: StateFlow<String?> = _transactionStatus.asStateFlow()
    
    private val _transactionHash = MutableStateFlow("")
    val transactionHash: StateFlow<String> = _transactionHash.asStateFlow()

    private val _blockConfirmations = MutableStateFlow(0)
    val blockConfirmations: StateFlow<Int> = _blockConfirmations.asStateFlow()

    // Chat messages
    private val _activeChatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val activeChatMessages: StateFlow<List<ChatMessageEntity>> = _activeChatMessages.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    // AI suggestions & insights state
    private val _aiRecommendationText = MutableStateFlow("")
    val aiRecommendationText: StateFlow<String> = _aiRecommendationText.asStateFlow()

    private val _isLoadingAiRecommendations = MutableStateFlow(false)
    val isLoadingAiRecommendations: StateFlow<Boolean> = _isLoadingAiRecommendations.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active Dashboard Tab
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    // Active Chat Product
    private val _chatProduct = MutableStateFlow<ProductEntity?>(null)
    val chatProduct: StateFlow<ProductEntity?> = _chatProduct.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateDataIfNeeded()
            // Check if profile exists and authenticate automatically for a smooth local demo experience
            val profile = repository.getProfile()
            if (profile != null) {
                _authState.value = AuthState.Authenticated(profile, walletConnected = true)
            }
        }
    }

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun selectChatProduct(product: ProductEntity?) {
        _chatProduct.value = product
        val prodId = product?.id ?: 0L
        viewModelScope.launch {
            repository.getChatMessages(prodId).collect {
                _activeChatMessages.value = it
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectProduct(product: ProductEntity?) {
        _selectedProduct.value = product
        if (product != null) {
            // Load chat messages for this product
            viewModelScope.launch {
                repository.getChatMessages(product.id).collect {
                    _activeChatMessages.value = it
                }
            }
        } else {
            // Load general/AI messages if product is null
            viewModelScope.launch {
                repository.getChatMessages(0).collect {
                    _activeChatMessages.value = it
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Auth flows
    fun simulateLogin(fullname: String, username: String, email: String, phone: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Authenticating
            val wallet = "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).uppercase() + "...A4e9"
            val profile = ProfileEntity(
                fullName = fullname,
                username = username,
                email = email,
                phoneNumber = phone,
                walletAddress = wallet,
                profileImage = "cyber_avatar_default",
                kycStatus = "UNVERIFIED",
                ethBalance = 5.0
            )
            repository.saveProfile(profile)
            _authState.value = AuthState.Authenticated(profile, walletConnected = true)
        }
    }

    fun simulateGoogleAuth() {
        viewModelScope.launch {
            _authState.value = AuthState.Authenticating
            // Emulate delay
            withContext(Dispatchers.IO) { Thread.sleep(1200) }
            val wallet = "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).uppercase() + "...3F81"
            val profile = ProfileEntity(
                fullName = "Core Operator",
                username = "core_ops_hologram",
                email = "operator@grid.net",
                phoneNumber = "+1 (777) MATRIX-1",
                walletAddress = wallet,
                profileImage = "cyber_avatar_google",
                kycStatus = "VERIFIED",
                ethBalance = 10.0
            )
            repository.saveProfile(profile)
            _authState.value = AuthState.Authenticated(profile, walletConnected = true)
        }
    }

    fun simulateWalletConnect(walletType: String) {
        viewModelScope.launch {
            _isConnectingWallet.value = true
            withContext(Dispatchers.IO) { Thread.sleep(1500) }
            val currentAuth = _authState.value
            if (currentAuth is AuthState.Authenticated) {
                // Update profile with wallet info
                val updated = currentAuth.profile.copy(
                    walletAddress = "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).uppercase()
                )
                repository.saveProfile(updated)
                _authState.value = AuthState.Authenticated(updated, walletConnected = true)
            } else {
                // Anonymous account connected via wallet
                val wallet = "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).uppercase()
                val profile = ProfileEntity(
                    fullName = "$walletType User",
                    username = "wallet_user_${walletType.lowercase()}",
                    email = "web3_anon@dma.network",
                    phoneNumber = "N/A",
                    walletAddress = wallet,
                    profileImage = "cyber_avatar_wallet",
                    kycStatus = "UNVERIFIED",
                    ethBalance = 3.52
                )
                repository.saveProfile(profile)
                _authState.value = AuthState.Authenticated(profile, walletConnected = true)
            }
            _isConnectingWallet.value = false
        }
    }

    fun simulateKYCRequest() {
        viewModelScope.launch {
            val currentProfile = repository.getProfile() ?: return@launch
            val updated = currentProfile.copy(kycStatus = "PENDING")
            repository.saveProfile(updated)
            
            // Emulate AI verification check
            withContext(Dispatchers.IO) { Thread.sleep(2500) }
            val finalProfile = updated.copy(kycStatus = "VERIFIED")
            repository.saveProfile(finalProfile)
            _authState.value = AuthState.Authenticated(finalProfile, walletConnected = true)
        }
    }

    fun toggleTwoFactor(enabled: Boolean) {
        viewModelScope.launch {
            val currentProfile = repository.getProfile() ?: return@launch
            val updated = currentProfile.copy(isTwoFactorEnabled = enabled)
            repository.saveProfile(updated)
            _authState.value = AuthState.Authenticated(updated, walletConnected = true)
        }
    }

    fun disconnectWallet() {
        viewModelScope.launch {
            val profile = repository.getProfile()
            if (profile != null) {
                val updated = profile.copy(walletAddress = "")
                repository.saveProfile(updated)
                _authState.value = AuthState.Authenticated(updated, walletConnected = false)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Delete profile & rebuild base state for clean demo reset
            _authState.value = AuthState.Unauthenticated
        }
    }

    // Buy Asset Flow
    fun buyAsset(product: ProductEntity) {
        viewModelScope.launch {
            val profile = repository.getProfile()
            if (profile == null || profile.walletAddress.isEmpty()) {
                _transactionStatus.value = "ERROR: Wallet not connected!"
                return@launch
            }
            if (profile.ethBalance < (product.priceEth + 0.0035)) {
                _transactionStatus.value = "ERROR: Insufficient ETH. Required: ${product.priceEth + 0.0035} ETH"
                return@launch
            }

            // Begin processing stages
            _transactionStatus.value = "ESTIMATING"
            delay(1000)
            
            _transactionStatus.value = "SIGNING"
            delay(1200)

            _transactionHash.value = "0x" + UUID.randomUUID().toString().replace("-", "")
            _transactionStatus.value = "BROADCASTING"
            delay(1000)

            _transactionStatus.value = "CONFIRMING"
            for (i in 1..4) {
                _blockConfirmations.value = i
                delay(800)
            }

            // Real update
            repository.buyProduct(
                productId = product.id,
                buyerWallet = profile.walletAddress,
                newOwnerWallet = profile.walletAddress,
                priceEth = product.priceEth,
                txHash = _transactionHash.value
            )

            _transactionStatus.value = "SUCCESS"
            
            // Reload selected item if it's the current one
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = repository.getProductById(product.id)
            }
        }
    }

    fun resetTransactionView() {
        val success = _transactionStatus.value == "SUCCESS"
        _transactionStatus.value = null
        _blockConfirmations.value = 0
        _transactionHash.value = ""
        if (success) {
            _activeTab.value = 0 // Automatically redirect back to Market Matrix (Tab 0)
        }
    }

    // Add Asset / Mint NFT Flow
    fun mintAsset(
        title: String,
        description: String,
        priceEth: Double,
        category: String,
        isCustomImage: Boolean,
        customImageUrl: String = ""
    ) {
        viewModelScope.launch {
            val profile = repository.getProfile() ?: return@launch
            _transactionStatus.value = "ESTIMATING"
            delay(1000)
            _transactionStatus.value = "SIGNING"
            delay(1000)

            val tx = "0x" + UUID.randomUUID().toString().replace("-", "")
            val tokenIdValue = (1000..9999).random().toString()
            val contractAddr = "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 40)
            val ipfsValue = "ipfs://Qm" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)

            val imageRef = when (category) {
                "NFT" -> "nft_ape"
                "Metaverse" -> "mv_penthouse"
                "Game Item" -> "wearable_blade"
                "Digital Art" -> "art_abstract"
                else -> "phys_jacket"
            }

            val newProduct = ProductEntity(
                title = title,
                description = description,
                priceEth = priceEth,
                priceUsd = priceEth * 3120.0, // Hardcoded multiplier $3120/ETH
                imageUrl = if (isCustomImage && customImageUrl.isNotBlank()) customImageUrl else imageRef,
                category = category,
                creatorWallet = profile.walletAddress,
                creatorName = profile.username,
                ownerWallet = profile.walletAddress,
                smartContractAddress = contractAddr,
                tokenId = tokenIdValue,
                status = "AVAILABLE",
                txHash = tx,
                ipfsHash = ipfsValue
            )

            repository.addProduct(newProduct)

            // Insert transaction record
            repository.addTransaction(
                TransactionEntity(
                    type = "MINT",
                    productTitle = title,
                    amountEth = 0.0,
                    txHash = tx,
                    senderWallet = "0x0000...0000",
                    receiverWallet = profile.walletAddress,
                    status = "SUCCESS",
                    gasPaidEth = 0.0028,
                    blockNumber = 18492100
                )
            )

            _transactionStatus.value = "SUCCESS"
        }
    }

    // Toggle Wishlist
    fun toggleProductWishlist(product: ProductEntity) {
        viewModelScope.launch {
            val newValue = !product.isWishlisted
            repository.toggleWishlist(product.id, newValue)
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = _selectedProduct.value?.copy(isWishlisted = newValue)
            }
        }
    }

    // Flag Asset Moderation
    fun flagProduct(id: Long, isFlagged: Boolean) {
        viewModelScope.launch {
            repository.flagProduct(id, isFlagged)
        }
    }

    // Message sending with AI assistant interaction
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val currentProfile = profileState.value ?: return
        val product = _chatProduct.value
        val prodId = product?.id ?: 0L

        viewModelScope.launch {
            _isSendingMessage.value = true
            
            // Insert user message
            val userMsg = ChatMessageEntity(
                productId = prodId,
                senderName = currentProfile.username,
                senderAddress = currentProfile.walletAddress,
                text = text,
                isAiAssistant = false
            )
            repository.sendChatMessage(userMsg)

            // If selectedProduct is null, or seller wallet matches 0x0000...0000 (meaning AI chat is selected), trigger AI response!
            // Let's check: AI dialog is active if product is null, or if user explicitly chats with sentinel.
            if (prodId == 0L) {
                // Query Gemini API
                val systemPrompt = """
                    You are DMA Sentinel, an AI-powered blockchain security, smart contract auditing, and escrow verification intelligence.
                    Your objective is to help the user understand smart contracts, identify potential honeypots or high slippage rugpull functions in Solidity, guide them on secure peer-to-peer escrow transactions, verify IPFS storage integrity, and explain Web3 metrics clearly.
                    Use a highly technical, futuristic, secure, and slightly cybernetic cyberpunk tone. Keep your responses complete but concise, with bullet points of security assessments.
                """.trimIndent()

                val aiResponseText = GeminiRetrofitClient.askGemini(
                    prompt = "User asks: $text. Analyze securely and answer in cyberpunk terminal style.",
                    systemPrompt = systemPrompt
                )

                repository.sendChatMessage(
                    ChatMessageEntity(
                        productId = 0,
                        senderName = "DMA AI Sentinel",
                        senderAddress = "0x0000...0000",
                        text = aiResponseText,
                        isAiAssistant = true
                    )
                )
            } else {
                // Standard peer-to-peer listing chat simulation: seller responds!
                delay(1500)
                val reply = when {
                    text.contains("price", ignoreCase = true) || text.contains("offer", ignoreCase = true) -> 
                        "Hello, thanks for the message! The price is slightly negotiable, but the gas fees are quite low right now. You can purchase directly via the secure Escrow Smart Contract."
                    text.contains("authenticity", ignoreCase = true) || text.contains("verify", ignoreCase = true) ->
                        "This asset is stored securely on IPFS with metadata verified at token ID #${product?.tokenId}. You can run a security check with our AI Sentinel!"
                    else -> 
                        "Hi there! Yes, the item '${product?.title}' is fully available. Feel free to initiate secure payment through the smart-contract. Let me know if you need any other technical details."
                }

                repository.sendChatMessage(
                    ChatMessageEntity(
                        productId = prodId,
                        senderName = product?.creatorName ?: "Seller",
                        senderAddress = product?.creatorWallet ?: "0xSeller",
                        text = reply,
                        isAiAssistant = false
                    )
                )
            }
            _isSendingMessage.value = false
        }
    }

    // Interactive Token Swap Simulation on Blockchain with real verification and balance update
    fun simulateSwap() {
        viewModelScope.launch {
            val profile = repository.getProfile()
            if (profile == null || profile.walletAddress.isEmpty()) {
                _transactionStatus.value = "ERROR: Wallet not connected!"
                return@launch
            }

            _transactionStatus.value = "ESTIMATING"
            delay(1000)
            
            _transactionStatus.value = "SIGNING"
            delay(1200)

            _transactionHash.value = "0x" + UUID.randomUUID().toString().replace("-", "")
            _transactionStatus.value = "BROADCASTING"
            delay(1000)

            _transactionStatus.value = "CONFIRMING"
            for (i in 1..4) {
                _blockConfirmations.value = i
                delay(800)
            }

            // Increase profile ETH balance by 2.0 ETH
            val updatedProfile = profile.copy(ethBalance = profile.ethBalance + 2.0)
            repository.saveProfile(updatedProfile)
            
            // Also notify Auth state
            _authState.value = AuthState.Authenticated(updatedProfile, walletConnected = true)

            // Add simple transaction record
            repository.addTransaction(
                TransactionEntity(
                    type = "DEPOSIT",
                    productTitle = "Liquidity Token Swap (+2.0 ETH)",
                    amountEth = 2.0,
                    txHash = _transactionHash.value,
                    senderWallet = "0x0000...0000",
                    receiverWallet = profile.walletAddress,
                    status = "SUCCESS",
                    gasPaidEth = 0.0012,
                    blockNumber = 18492080
                )
            )

            _transactionStatus.value = "SUCCESS"
        }
    }

    // Gemini based item recommendations
    fun generateAiRecommendations() {
        viewModelScope.launch {
            _isLoadingAiRecommendations.value = true
            _aiRecommendationText.value = "Analyzing ledger activity, current market indices and matching IPFS metadata hashes..."
            
            val productsList = productsState.value
            val profile = profileState.value
            if (productsList.isEmpty()) {
                _aiRecommendationText.value = "Market inventory is empty. No recommendations could be calculated."
                _isLoadingAiRecommendations.value = false
                return@launch
            }

            val productsOverview = productsList.joinToString("\n") { 
                "- [${it.category}] Title: ${it.title}, Price: ${it.priceEth} ETH, Token: #${it.tokenId}, ID: ${it.id}"
            }

            val prompt = """
                Based on user profile: ${profile?.username ?: "Anonymous Operator"} with balance ${profile?.ethBalance ?: 0.0} ETH.
                Here is our blockchain marketplace database inventory list:
                $productsOverview
                
                Choose the 2 single best matching items from this specific list above, explain exactly why they fit a cyberpunk collector, analyze their smart contract liquidity securely, and write a concise, visually striking recommendation pitch.
            """.trimIndent()

            val response = GeminiRetrofitClient.askGemini(
                prompt = prompt,
                systemPrompt = "You are the AI Quantum Recommendation Indexer. Provide elite intelligence regarding market purchases. Be futuristic, concise, and structured."
            )
            
            _aiRecommendationText.value = response
            _isLoadingAiRecommendations.value = false
        }
    }

    private suspend fun delay(ms: Long) {
        withContext(Dispatchers.IO) {
            Thread.sleep(ms)
        }
    }
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
