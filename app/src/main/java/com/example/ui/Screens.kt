package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ChatMessageEntity
import com.example.data.ProductEntity
import com.example.data.TransactionEntity
import com.example.ui.theme.*

// Helper for cyberpunk styled neon borders
fun Modifier.cyberBorder(
    color: Color,
    cornerRadius: Dp = 12.dp,
    strokeWidth: Float = 4f,
    glowRadius: Float = 10f
) = this.drawWithContent {
    drawContent()
    // Neon glow effect (soft blur represented by multiple concentric borders)
    for (i in 1..3) {
        drawRoundRect(
            color = color.copy(alpha = 0.12f / i),
            topLeft = Offset(-strokeWidth * i, -strokeWidth * i),
            size = Size(size.width + strokeWidth * 2 * i, size.height + strokeWidth * 2 * i),
            cornerRadius = CornerRadius(cornerRadius.toPx() + strokeWidth * i),
            style = Stroke(width = strokeWidth + i)
        )
    }
    // Main sharp neon stroke
    drawRoundRect(
        color = color,
        topLeft = Offset(0f, 0f),
        size = size,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(width = strokeWidth)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalMarketAppScreen(viewModel: MainViewModel) {
    val authState by viewModel.authState.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()

    MyApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CyberDark, Color(0xFF070B18), Color(0xFF0F081C))
                    )
                )
        ) {
            // Render grid structure to mimic cyber matrix background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 120f
                val strokeColor = GridColor
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawLine(strokeColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                }
                for (y in 0..size.height.toInt() step step.toInt()) {
                    drawLine(strokeColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                }
            }

            AnimatedContent(
                targetState = authState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "MainAuthSwitch"
            ) { state ->
                when (state) {
                    is AuthState.Unauthenticated, AuthState.Authenticating -> {
                        AuthScreen(
                            isLoading = state is AuthState.Authenticating,
                            onLoginSubmit = { name, bname, email, phone ->
                                viewModel.simulateLogin(name, bname, email, phone)
                            },
                            onGoogleClick = { viewModel.simulateGoogleAuth() },
                            onWalletClick = { viewModel.simulateWalletConnect("MetaMask") }
                        )
                    }
                    is AuthState.Authenticated -> {
                        if (selectedProduct != null) {
                            ProductDetailScreen(
                                product = selectedProduct!!,
                                viewModel = viewModel,
                                onBack = { viewModel.selectProduct(null) }
                            )
                        } else {
                            MainHubDashboard(viewModel = viewModel, profile = state.profile)
                        }
                    }
                }
            }

            // Global Transaction Overlay Dialog
            val txStatus by viewModel.transactionStatus.collectAsState()
            if (txStatus != null) {
                BlockchainProcessingDialog(
                    status = txStatus!!,
                    txHash = viewModel.transactionHash.collectAsState().value,
                    confirmations = viewModel.blockConfirmations.collectAsState().value,
                    onDismiss = { viewModel.resetTransactionView() }
                )
            }
        }
    }
}

@Composable
fun AuthScreen(
    isLoading: Boolean,
    onLoginSubmit: (String, String, String, String) -> Unit,
    onGoogleClick: () -> Unit,
    onWalletClick: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .navigationBarsPadding()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .cyberBorder(CyberCyan, 16.dp)
                .background(CyberSurface.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Neon Brand Title
            Icon(
                imageVector = Icons.Filled.Hub,
                contentDescription = "DMA logo",
                tint = CyberCyan,
                modifier = Modifier
                    .size(64.dp)
                    .drawWithContent {
                        drawContent()
                    }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "DIGITAL MARKET ASSET",
                color = Color.White,
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "AI-POWERED WEB3 MATRIX",
                color = CyberPink,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "DECRYPTING PRIVATE METADATA...",
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            } else {
                if (isSignUpMode) {
                    TextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("auth_fullname"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CyberCard,
                            unfocusedContainerColor = CyberCard,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = SoftGrey,
                            focusedIndicatorColor = CyberCyan
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth().testTag("auth_username"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CyberCard,
                            unfocusedContainerColor = CyberCard,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = SoftGrey,
                            focusedIndicatorColor = CyberCyan
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Hash") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CyberCard,
                            unfocusedContainerColor = CyberCard,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = SoftGrey,
                            focusedIndicatorColor = CyberCyan
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Quantum Comms (Phone)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CyberCard,
                            unfocusedContainerColor = CyberCard,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = SoftGrey,
                            focusedIndicatorColor = CyberCyan
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (fullName.isNotBlank() && username.isNotBlank()) {
                                onLoginSubmit(fullName, username, email, phone)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_registration"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("INITIALIZE PROTOCOL", color = CyberDark, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    // Quick Direct Demo Sign-in for immediate access
                    Button(
                        onClick = {
                            onLoginSubmit("Core Operator", "admin_ops_cyber", "ops@dma.network", "+1-800-NET-GRID")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_quick_login"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("AUTHORIZED DECRYPT MODE", color = CyberDark, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onGoogleClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurface, contentColor = Color.White),
                        border = BorderStroke(1.dp, SoftGrey),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.AlternateEmail, contentDescription = "Google", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("DECRYPT VIA GOOGLE ID", fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onWalletClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("connect_wallet_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.AccountBalanceWallet, contentDescription = "Wallet")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("CONNECT WEB3 WALLET", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { isSignUpMode = !isSignUpMode }
                ) {
                    Text(
                        text = if (isSignUpMode) "SWITCH TO STANDARD DECRYPT PORT" else "CREATE NEW BIO-DIGITAL ID",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MainHubDashboard(viewModel: MainViewModel, profile: com.example.data.ProfileEntity) {
    val activeTab by viewModel.activeTab.collectAsState()
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CyberDark,
                modifier = Modifier
                    .cyberBorder(CyberCyan, 0.dp, strokeWidth = 2f)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    Triple("Matrix", Icons.Default.GridView, 0),
                    Triple("AI Chat", Icons.Default.Security, 1),
                    Triple("Mint", Icons.Default.AddBox, 2),
                    Triple("Ledger", Icons.Default.Analytics, 3),
                    Triple("Operator", Icons.Default.Face, 4),
                    Triple("Admin", Icons.Default.SettingsInputComponent, 5)
                )
                tabs.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = activeTab == index,
                        onClick = { viewModel.setActiveTab(index) },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            unselectedIconColor = SoftGrey,
                            selectedTextColor = CyberCyan,
                            unselectedTextColor = SoftGrey,
                            indicatorColor = CyberSurface
                        ),
                        modifier = Modifier.testTag("tab_$label")
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Decent Ticker Header
            BlockchainTickerHeader(profile = profile, viewModel = viewModel)

            Box(modifier = Modifier.fillMaxSize()) {
                when (activeTab) {
                    0 -> MarketTab(viewModel = viewModel)
                    1 -> ChatTab(viewModel = viewModel)
                    2 -> MintTab(viewModel = viewModel)
                    3 -> LedgerTab(viewModel = viewModel)
                    4 -> ProfileTab(viewModel = viewModel, profile = profile)
                    5 -> AdminTab(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun BlockchainTickerHeader(profile: com.example.data.ProfileEntity, viewModel: MainViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Brand Header Row (Directly from Design HTML)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DIGITAL MARKET ASSET",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "Marketplace Hub",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Dynamic Ticker Capsule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0x1A00F0FF), CircleShape)
                    .border(1.dp, CyberCyan.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(CyberCyan.copy(alpha = alphaAnim), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MAINNET",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Gorgeous Connected Wallet Card (Directly from Design HTML Styling)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
                .border(BorderStroke(1.dp, Color(0x3394A3B8)), RoundedCornerShape(24.dp))
                .drawBehind {
                    // Accent cyan radial lighting spot (absolute top[-20%] right[-10%] blur style)
                    drawCircle(
                        color = CyberCyan.copy(alpha = 0.08f),
                        radius = size.width * 0.4f,
                        center = Offset(size.width * 1.1f, size.height * -0.2f)
                    )
                }
                .padding(20.dp)
        ) {
            Column {
                // Connected status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Connected Wallet",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = if (profile.walletAddress.isNotBlank()) {
                                    val addr = profile.walletAddress
                                    if (addr.length > 12) "${addr.take(6)}...${addr.takeLast(4)}" else addr
                                } else {
                                    "0x71C...4F2d"
                                },
                                color = CyberCyan,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                            )
                        }
                    }
                    
                    // Gas / Cost Indicator badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0x33475569), CircleShape)
                            .border(1.dp, Color(0x6694A3B8), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = "Gas",
                            tint = CyberCyan,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "18 GWEI",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Balance and Action Swap Button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Total Balance",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = String.format("%.3f", profile.ethBalance),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "ETH",
                                color = SoftGrey,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }

                    // Theme Styled Action Button
                    Button(
                        onClick = { viewModel.simulateSwap() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("wallet_swap_button")
                    ) {
                        Text(
                            text = "SWAP",
                            color = Color(0xFF0F172A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ------ TABS ------

@Composable
fun MarketTab(viewModel: MainViewModel) {
    val products by viewModel.productsState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showSecurityAlert = remember { mutableStateOf(false) }

    val categories = listOf("All", "NFT", "Metaverse", "Game Item", "Digital Art", "Physical")

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // AI Recommendations Trigger Box
        AiIntelligenceWidget(viewModel = viewModel)

        // Custom Cyberpunk Search Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("SEARCH ASSET DECENTRALIZED REPO...", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "sh", tint = CyberCyan) },
                modifier = Modifier.weight(1f).testTag("market_search_input"),
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "clear")
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = CyberCyan,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { showSecurityAlert.value = true },
                modifier = Modifier
                    .background(CyberSurface, RoundedCornerShape(8.dp))
                    .cyberBorder(CyberPink, 8.dp, strokeWidth = 2f)
            ) {
                Icon(imageVector = Icons.Default.OfflineBolt, contentDescription = "Contract Analyzer", tint = CyberPink)
            }
        }

        // Category Hub List (Scrollable Row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val active = selectedCategory == cat
                Button(
                    onClick = { viewModel.setCategory(cat) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) CyberCyan else Color(0x331E293B),
                        contentColor = if (active) Color(0xFF0F172A) else Color(0xFFCBD5E1)
                    ),
                    border = BorderStroke(1.dp, if (active) CyberCyan else Color(0xFF334155)),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("category_tab_$cat")
                ) {
                    Text(cat.uppercase(), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Filter product items based on Category & Search
        val filteredList = products.filter {
            (selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)) &&
            (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)) &&
            !it.isFlagged
        }

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = "empty", tint = SoftGrey, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "NO COMPATIBLE ASSETS IN CURRENT MATRIX",
                        color = SoftGrey,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                items(filteredList) { product ->
                    ProductCard(product = product, onSelect = { viewModel.selectProduct(product) })
                }
            }
        }
    }

    if (showSecurityAlert.value) {
        Dialog(onDismissRequest = { showSecurityAlert.value = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberBorder(CyberPink, 16.dp)
                    .background(CyberDark, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "sec", tint = CyberPink, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "DECENTRALIZED MEMORY ANALYZER",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Decentralized wallets on DMA use an Escrow security pool. Real funds are never immediately broadcasted until the AI validator and buyer confirm asset delivery or IPFS hash integrity.",
                        color = SoftGrey,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showSecurityAlert.value = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPink)
                    ) {
                        Text("ACKNOWLEDGEMENT SIGNED", color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun AiIntelligenceWidget(viewModel: MainViewModel) {
    val isRecommendationLoading by viewModel.isLoadingAiRecommendations.collectAsState()
    val recommendationText by viewModel.aiRecommendationText.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cyberBorder(if (expanded) CyberPurple else ElectricBlue, 8.dp, strokeWidth = 2f)
            .background(CyberSurface.copy(alpha = 0.8f))
            .padding(12.dp)
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "ai", tint = CyberPurple, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI QUANTUM PORTFOLIO SUGGESTIONS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "expand",
                tint = SoftGrey,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                if (recommendationText.isBlank()) {
                    Text(
                        text = "Trigger deep neural ledger indices to discover undervalued assets matching your digital wallet balance and metadata preferences.",
                        color = SoftGrey,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberDark, RoundedCornerShape(4.dp))
                            .border(1.dp, CyberPurple.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = recommendationText,
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = { viewModel.generateAiRecommendations() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                    modifier = Modifier.fillMaxWidth().testTag("ai_recs_button"),
                    enabled = !isRecommendationLoading
                ) {
                    if (isRecommendationLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("DECRYPT PORTFOLIO RECOMMENDATIONS", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// Compact Grid Product Card
@Composable
fun ProductCard(product: ProductEntity, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clickable { onSelect() }
            .cyberBorder(
                color = when (product.category) {
                    "NFT" -> CyberCyan
                    "Metaverse" -> CyberPink
                    "Game Item" -> CyberPurple
                    else -> ElectricBlue
                },
                cornerRadius = 20.dp,
                strokeWidth = 1.5f
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x331E293B)),
        border = BorderStroke(1.dp, Color(0x1F94A3B8))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                // Synthesize decorative vector placeholder inside cards to bypass Coil loading fail risks
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gradientColors = when (product.category) {
                        "NFT" -> listOf(Color(0xFF6366F1), Color(0xFF9333EA))
                        "Metaverse" -> listOf(Color(0xFFEC4899), Color(0xFFF97316))
                        "Game Item" -> listOf(Color(0xFF00F0FF), Color(0xFF3B82F6))
                        "Digital Art" -> listOf(Color(0xFF8B5CF6), Color(0xFFD946EF))
                        else -> listOf(Color(0xFF10B981), Color(0xFF06B6D4))
                    }
                    val brush = Brush.linearGradient(colors = gradientColors)
                    drawRect(brush)
                    
                    // Draw digital cyber circuit diagram overlay with soft circular overlay for depth
                    drawCircle(Color.White.copy(alpha = 0.15f), radius = size.minDimension / 1.5f, center = Offset(size.width/2, size.height/2))
                    drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = 1.5f)
                }
                
                // Show clean neon category stamp
                Text(
                    text = product.category.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(CyberPink, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )

                Icon(
                    imageVector = when (product.category) {
                        "NFT" -> Icons.Default.Image
                        "Metaverse" -> Icons.Default.Layers
                        "Game Item" -> Icons.Default.ColorLens
                        "Digital Art" -> Icons.Default.Casino
                        else -> Icons.Default.ShoppingBag
                    },
                    contentDescription = "preview",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@${product.creatorName}",
                    color = SoftGrey,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CurrencyBitcoin, contentDescription = "eth", tint = CyberCyan, modifier = Modifier.size(12.dp))
                            Text(
                                text = "${product.priceEth} ETH",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "≈ $${product.priceUsd.toInt()}",
                            color = SoftGrey,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Simulated ESCROW Secure status tag
                    Row(
                        modifier = Modifier
                            .background(Color(0x3322C55E), RoundedCornerShape(6.dp))
                            .border(1.dp, CyberGlow.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = "esc", tint = CyberGlow, modifier = Modifier.size(8.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("ESCROW", color = CyberGlow, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ------ CHAT / AI SENTINEL TAB ------

@Composable
fun ChatTab(viewModel: MainViewModel) {
    val messages by viewModel.activeChatMessages.collectAsState()
    val isSending by viewModel.isSendingMessage.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val chatProduct by viewModel.chatProduct.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Conversation Header Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .cyberBorder(if (chatProduct != null) CyberPink else CyberPurple, 8.dp, strokeWidth = 2f),
            colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.80f))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (chatProduct != null) CyberPink.copy(alpha = 0.2f) else CyberPurple.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, if (chatProduct != null) CyberPink else CyberPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (chatProduct != null) Icons.Default.Forum else Icons.Default.Security,
                        contentDescription = "chat target",
                        tint = if (chatProduct != null) CyberPink else CyberPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (chatProduct != null) {
                            "PEER CHAT: @${chatProduct!!.creatorName}"
                        } else {
                            "DMA AI SENTINEL & WEB3 SECURE"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (chatProduct != null) {
                            "Product: ${chatProduct!!.title} (Token #${chatProduct!!.tokenId})"
                        } else {
                            "Quantum Ledger Audit & Fraud Verification Console"
                        },
                        color = SoftGrey,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                if (chatProduct != null) {
                    IconButton(
                        onClick = { viewModel.selectChatProduct(null) }
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "return", tint = SoftGrey)
                    }
                }
            }
        }

        // Preset Prompt Quick Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val templates = if (chatProduct != null) {
                listOf(
                    "Is the price of ${chatProduct!!.title} negotiable?",
                    "Can you verify the IPFS metadata content?",
                    "What makes this NFT asset completely authentic?",
                    "Is escrow protection enabled for this item?"
                )
            } else {
                listOf(
                    "Run Smart Contract check for Rugpull triggers.",
                    "Estimate current optimal Polygon Gas fee metrics.",
                    "Verify decentralized IPFS file hash certificate.",
                    "Explain how escrow payment security is locked."
                )
            }
            templates.forEach { prompt ->
                Button(
                    onClick = { viewModel.sendMessage(prompt) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                    border = BorderStroke(1.dp, if (chatProduct != null) CyberPink.copy(alpha = 0.4f) else CyberPurple.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(prompt, fontSize = 9.sp, color = CyberCyan, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Conversation List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatMessageBubble(message = message)
            }
        }

        // Input Field Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberSurface, RoundedCornerShape(8.dp))
                .border(2.dp, if (chatProduct != null) CyberPink else CyberPurple, RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { 
                    Text(
                        text = if (chatProduct != null) "Send secure offer/message to asset owner..." else "Query cryptographic Sentinel audit interface...",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SoftGrey
                    ) 
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = !isSending,
                modifier = Modifier.testTag("chat_send_button")
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(18.dp))
                } else {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "send", tint = CyberCyan)
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessageEntity) {
    val isUser = !message.isAiAssistant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message.senderName,
                color = if (isUser) CyberCyan else CyberPink,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = message.senderAddress,
                color = SoftGrey,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .background(
                    if (isUser) CyberCard else CyberSurface,
                    RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = if (isUser) 8.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 8.dp
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) CyberCyan.copy(alpha = 0.5f) else CyberPink.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = if (isUser) 8.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 8.dp
                    )
                )
                .padding(10.dp)
        ) {
            Text(
                text = message.text,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ------ MINT / UPLOAD TAB ------

@Composable
fun MintTab(viewModel: MainViewModel) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceEthStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("NFT") }
    var isCustomImage by remember { mutableStateOf(false) }
    var customImageUrl by remember { mutableStateOf("") }

    val categories = listOf("NFT", "Metaverse", "Game Item", "Digital Art", "Physical")

    // Dynamic cost feedback calculation
    val baseGas = 0.0028
    val calculatedGas = baseGas + (title.length + description.length) * 0.00001
    val ethPrice = 3120.0
    val costUsd = calculatedGas * ethPrice

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(
                text = "MINT DECENTRALIZED SMART ASSET",
                color = CyberPink,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Compose metadata structure to index asset, claim direct ownership and initialize automatic escrow parameters.",
                color = SoftGrey,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurface, RoundedCornerShape(12.dp))
                    .cyberBorder(CyberPink, 12.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Asset Title (e.g. Neon Skull NFT)") },
                    modifier = Modifier.fillMaxWidth().testTag("mint_title_input"),
                    colors = TextFieldDefaults.colors(focusedContainerColor = CyberCard, unfocusedContainerColor = CyberCard, focusedTextColor = Color.White)
                )

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Dynamic Metadata Description") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = CyberCard, unfocusedContainerColor = CyberCard, focusedTextColor = Color.White)
                )

                TextField(
                    value = priceEthStr,
                    onValueChange = { priceEthStr = it },
                    label = { Text("Asset Price (ETH)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag("mint_price_input"),
                    colors = TextFieldDefaults.colors(focusedContainerColor = CyberCard, unfocusedContainerColor = CyberCard, focusedTextColor = Color.White)
                )

                // Category selection Dropdown replacement
                Text("Select Target Category Ledger:", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberPink,
                                selectedLabelColor = Color.White,
                                disabledContainerColor = CyberSurface
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isCustomImage,
                        onCheckedChange = { isCustomImage = it },
                        colors = CheckboxDefaults.colors(checkedColor = CyberPink)
                    )
                    Text("Register External Image URL", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                if (isCustomImage) {
                    TextField(
                        value = customImageUrl,
                        onValueChange = { customImageUrl = it },
                        label = { Text("MIME Image Web Address") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedContainerColor = CyberCard, unfocusedContainerColor = CyberCard, focusedTextColor = Color.White)
                    )
                }
            }
        }

        // Live Block Simulation Cost Metrics Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberCard.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gas Estimation Limit:", color = SoftGrey, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("210,000 NanoUnits", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Smart Contract Mint Fee:", color = SoftGrey, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("${String.format("%.5f", calculatedGas)} ETH", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Calculated Fiat Cost:", color = SoftGrey, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("≈ $${String.format("%.2f", costUsd)} USD", color = CyberPink, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    val price = priceEthStr.toDoubleOrNull()
                    if (title.isNotBlank() && price != null && price > 0) {
                        viewModel.mintAsset(title, description, price, category, isCustomImage, customImageUrl)
                        // Reset forms
                        title = ""
                        description = ""
                        priceEthStr = ""
                        isCustomImage = false
                        customImageUrl = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("mint_submit_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.PrecisionManufacturing, contentDescription = "mint icon", tint = CyberDark)
                Spacer(modifier = Modifier.width(10.dp))
                Text("INTEGRATE & SIGN METADATA CALL", color = CyberDark, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ------ LEDGER HISTORIC TRANSACTIONS ------

@Composable
fun LedgerTab(viewModel: MainViewModel) {
    val txs by viewModel.transactionsState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "DECENTRALIZED QUANTUM LEDGER",
            color = CyberCyan,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Real-time transparent cryptographic blockchain records of the Platform's minting and transferring actions.",
            color = SoftGrey,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        if (txs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("NO TRANSACTION BLOCKS VERIFIED IN LEDGER YET", color = SoftGrey, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(txs) { tx ->
                    LedgerRecordItem(tx = tx)
                }
            }
        }
    }
}

@Composable
fun LedgerRecordItem(tx: TransactionEntity) {
    val clipboard = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .cyberBorder(if (tx.type == "MINT") CyberCyan else CyberPink, 8.dp, strokeWidth = 2f),
        colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (tx.type == "MINT") Icons.Default.BuildCircle else Icons.Default.ReceiptLong,
                        contentDescription = "type",
                        tint = if (tx.type == "MINT") CyberCyan else CyberPink,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tx.type.uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "BLOCK #${tx.blockNumber}",
                    color = CyberGlow,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = GridColor)
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Target: ${tx.productTitle}",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gas Cost limit paid:", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("${tx.gasPaidEth} ETH", color = CyberCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Acquisition Value:", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("${tx.amountEth} ETH", color = CyberPink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clickable transaction hash copy link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(CyberCard, RoundedCornerShape(4.dp))
                    .clickable { clipboard.setText(AnnotatedString(tx.txHash)) }
                    .padding(6.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "copy", tint = CyberCyan, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TXN HASH: ${tx.txHash.take(16)}...${tx.txHash.takeLast(6)}",
                    color = CyberCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ------ PROFILE / OPERATOR CONSOLE TAB ------

@Composable
fun ProfileTab(viewModel: MainViewModel, profile: com.example.data.ProfileEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Identity Certificate Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .cyberBorder(CyberCyan, 12.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(CyberCyan.copy(alpha = 0.2f), CircleShape)
                            .border(2.dp, CyberCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Face, contentDescription = "operator avatar", tint = CyberCyan, modifier = Modifier.size(48.dp))
                    }
                    // KYC status light beacon
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = when (profile.kycStatus) {
                                    "VERIFIED" -> CyberGlow
                                    "PENDING" -> CyberGold
                                    else -> CyberPink
                                },
                                shape = CircleShape
                            )
                            .border(2.dp, CyberDark, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = profile.fullName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "@${profile.username}",
                    color = SoftGrey,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Followers vs Following stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${profile.followersCount}", color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(text = "FOLLOWERS", color = SoftGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(SoftGrey))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${profile.followingCount}", color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(text = "FOLLOWING", color = SoftGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(SoftGrey))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${profile.sellerRating}★", color = CyberGold, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(text = "RATING", color = SoftGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Action controls for KYC or 2FA
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .cyberBorder(CyberPink, 8.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "BIOMETRIC KYC DECRYPTION",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Verify legal identity attributes to secure higher liquidity thresholds inside decentralized ledger operations.",
                    color = SoftGrey,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Button(
                    onClick = { viewModel.simulateKYCRequest() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (profile.kycStatus) {
                            "VERIFIED" -> CyberGlow
                            "PENDING" -> CyberGold
                            else -> CyberPink
                        }
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("kyc_trigger_button"),
                    enabled = profile.kycStatus != "VERIFIED" && profile.kycStatus != "PENDING"
                ) {
                    Text(
                        text = when (profile.kycStatus) {
                            "VERIFIED" -> "IDENTITY BIOMETRICS COMPLETE [VERIFIED]"
                            "PENDING" -> "DECRYPTING IDENTITY CREDENTIALS...[PENDING]"
                            else -> "RUN BIOMETRIC DECRYPT PROTOCOL"
                        },
                        color = if (profile.kycStatus == "VERIFIED") CyberDark else Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2FA Security Switch card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GridColor, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Quantum 2-Factor Authentication", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                    Text("Requires physical hardware code signature to run trades", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Switch(
                    checked = profile.isTwoFactorEnabled,
                    onCheckedChange = { viewModel.toggleTwoFactor(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                )
            }
        }

        // Email and phone descriptors
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GridColor, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Digital COMMS Node:", color = SoftGrey, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(profile.phoneNumber, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Registered Contact Email:", color = SoftGrey, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(profile.email, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Wallet disconnecting trigger
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = CyberDark),
            border = BorderStroke(1.dp, CyberPink),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("logout_button")
        ) {
            Text("WIPE BIO-ID AND EXIT MATRIX", color = CyberPink, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

// ------ ADMIN ANALYTICS TAB ------

@Composable
fun AdminTab(viewModel: MainViewModel) {
    val products by viewModel.productsState.collectAsState()
    val transactions by viewModel.transactionsState.collectAsState()

    // Calculate active stats
    var mintVolume = 0.0
    var purchaseVolume = 0.0
    transactions.forEach {
        if (it.type == "PURCHASE" || it.type == "ESCROW_RELEASE") {
            purchaseVolume += it.amountEth
        } else if (it.type == "MINT") {
            mintVolume += it.gasPaidEth
        }
    }

    val totalContractGasPaid = transactions.sumOf { it.gasPaidEth }
    val totalRevenueContractEth = purchaseVolume * 0.025 + totalContractGasPaid * 0.1 // 2.5% platform protocol tax fee

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "DMA QUANTUM CENTRAL CONSOLE",
                color = CyberGold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Track active platform liquidity matrix, escrow volume indices, gas metrics, and moderate smart contracts.",
                color = SoftGrey,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        item {
            // Dashboard grid cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("CONTRACT VOLUME", color = SoftGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("${String.format("%.3f", purchaseVolume)} ETH", color = CyberCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("≈ $${(purchaseVolume * 3120).toInt()} USD", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).border(1.dp, CyberPink.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("PLATFORM TAX REVENUE", color = SoftGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("${String.format("%.4f", totalRevenueContractEth)} ETH", color = CyberPink, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("≈ $${(totalRevenueContractEth * 3120).toInt()} USD", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).border(1.dp, CyberPurple.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("INDEXED PRODUCTS", color = SoftGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("${products.size} ASSETS", color = CyberPurple, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("Escrow Pool Protected", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).border(1.dp, CyberGlow.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("DECEN TRANSACTIONS", color = SoftGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("${transactions.size} BLOCKS", color = CyberGlow, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("Avg Latency 14.5s", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Draw custom interactive glowing Line Chart via Jetpack Compose canvas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberBorder(CyberCyan, 12.dp)
                    .background(CyberSurface.copy(alpha = 0.8f))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "NETWORK TRANSACTION FLOW TREND (24H)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        // Drawing simulated crypto grid trend
                        val pts = listOf(20f, 60f, 40f, 95f, 75f, 130f, 110f, 160f, 140f, 210f)
                        val wUnit = size.width / (pts.size - 1)
                        val hScale = size.height / 250f

                        // Draw background helper guidelines
                        for (i in 0..4) {
                            val lineY = size.height - (i * size.height / 4)
                            drawLine(GridColor, Offset(0f, lineY), Offset(size.width, lineY), strokeWidth = 1f)
                        }

                        // Drawing glowing trend line
                        for (i in 0 until pts.size - 1) {
                            val p1 = Offset(i * wUnit, size.height - pts[i] * hScale)
                            val p2 = Offset((i + 1) * wUnit, size.height - pts[i + 1] * hScale)
                            
                            // Draw neon broad blur
                            drawLine(CyberCyan.copy(alpha = 0.25f), p1, p2, strokeWidth = 12f)
                            // Draw neon core
                            drawLine(CyberCyan, p1, p2, strokeWidth = 4f)
                            
                            // Draw glow dots
                            drawCircle(CyberPink, radius = 5f, center = p1)
                        }
                        drawCircle(CyberPink, radius = 7f, center = Offset((pts.size - 1) * wUnit, size.height - pts.last() * hScale))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("00:00 H", color = SoftGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("12:00 H", color = SoftGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("24:00 H", color = SoftGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Mock product list for simple moderation (flag / verify / report)
        item {
            Text(
                text = "INDEXED ASSET SECURITY REPOSITORIES",
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        if (products.isEmpty()) {
            item {
                Text(text = "No active listed assets", color = SoftGrey, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        } else {
            items(products) { prod ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberCard, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prod.title, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("Creator ID: @${prod.creatorName}", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Price: ${prod.priceEth} ETH", color = CyberCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = { viewModel.flagProduct(prod.id, !prod.isFlagged) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (prod.isFlagged) CyberGlow else CyberPink),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (prod.isFlagged) "RE-INDEX" else "FLAG FRAUD",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (prod.isFlagged) CyberDark else Color.White
                        )
                    }
                }
            }
        }
    }
}

// ------ PRODUCT DETAIL SCREEN ------

@Composable
fun ProductDetailScreen(
    product: ProductEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var isVerifiedReportOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberDark)
                    .statusBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = CyberCyan)
                }
                Text(
                    text = "ASSET MATRIC DETAIL",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual NFT / Asset Graphic representation card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(CyberSurface, RoundedCornerShape(12.dp))
                    .cyberBorder(CyberPink, 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        Brush.sweepGradient(
                            colors = listOf(CyberDark, CyberSurface, CyberCard, CyberDark)
                        )
                    )
                    drawCircle(
                        color = CyberPink.copy(alpha = 0.2f),
                        radius = 160f
                    )
                    // Draw decorative tech matrix symbols
                    drawLine(CyberCyan.copy(alpha = 0.3f), Offset(100f, 0f), Offset(100f, size.height), strokeWidth = 1f)
                    drawLine(CyberCyan.copy(alpha = 0.3f), Offset(size.width - 100f, 0f), Offset(size.width - 100f, size.height), strokeWidth = 1f)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Cyclone,
                        contentDescription = "cyber preview nft",
                        tint = CyberPink,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "METADATA LEDGER VERIFIED",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Title & category segment
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.category.uppercase(),
                        color = CyberPink,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    // Wishlist visual button
                    IconButton(
                        onClick = { viewModel.toggleProductWishlist(product) },
                        modifier = Modifier.testTag("wishlist_button")
                    ) {
                        Icon(
                            imageVector = if (product.isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "wishlist toggle",
                            tint = CyberPink
                        )
                    }
                }
                Text(
                    text = product.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MINTED BY OPERATOR: @${product.creatorName}",
                    color = SoftGrey,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Price segment card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GridColor, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ACQUISITION EXCHANGE VALUE", color = SoftGrey, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CurrencyBitcoin, contentDescription = "eth", tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Text(
                                text = "${product.priceEth} ETH",
                                color = CyberCyan,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text("≈ $${product.priceUsd.toInt()} USD", color = SoftGrey, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    // Simulated secure status
                    if (product.status == "SOLD") {
                        Box(
                            modifier = Modifier
                                .background(CyberCard, RoundedCornerShape(4.dp))
                                .border(1.dp, CyberPink, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("SOLD OUT", color = CyberPink, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.buyAsset(product) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier.testTag("buy_submit_button")
                        ) {
                            Text("INITIALIZE SECURE ESCROW BUY", color = CyberDark, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Description and verified contract fields
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LEDGER METADATA SPECIFICATIONS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(
                        text = product.description,
                        color = SoftGrey,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Verified Smart Contract Address
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberBorder(CyberCyan, 8.dp, strokeWidth = 2f),
                colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.85f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("VERIFIED INTEGRITY SPECIFICATIONS", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Contract Address:", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${product.smartContractAddress.take(8)}...${product.smartContractAddress.takeLast(6)}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable { clipboard.setText(AnnotatedString(product.smartContractAddress)) }
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IPFS Decentralized Storage Hash:", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${product.ipfsHash.take(12)}...",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable { clipboard.setText(AnnotatedString(product.ipfsHash)) }
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Token Identity ID Index:", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("#${product.tokenId}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Blockchain Registry Txt:", color = SoftGrey, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${product.txHash.take(10)}...",
                            color = CyberPink,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable { clipboard.setText(AnnotatedString(product.txHash)) }
                        )
                    }
                }
            }

            // AI Smart Security Inspector Analysis trigger
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberBorder(CyberPurple, 8.dp, strokeWidth = 2f)
                    .background(CyberSurface.copy(alpha = 0.8f))
                    .padding(12.dp)
                    .clickable { isVerifiedReportOpen = !isVerifiedReportOpen }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "auto check", tint = CyberPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI CODE INSPECTOR: AUDIT SMART CONTRACT",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Icon(imageVector = if (isVerifiedReportOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "exp", tint = SoftGrey)
                    }

                    if (isVerifiedReportOpen) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberDark, RoundedCornerShape(4.dp))
                                .border(1.dp, CyberPurple.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Contract Address verified securely. Verified metadata exists under standard ERC-721 ledger rules. Escrow lock initialized successfully at Block Number #${product.blockNumber}. Code review shows zero reentrancy vulnerability or rugpull metrics. Status: EXTREMELY SECURE FOR DEPLOYMENT.",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Direct Peer-to-Peer Chat button
            Button(
                onClick = {
                    viewModel.selectChatProduct(product)
                    viewModel.setActiveTab(1) // Open AI Chat Tab which adapts to product
                    viewModel.selectProduct(null) // Return to main hub dashboard
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurface, contentColor = Color.White),
                border = BorderStroke(1.dp, CyberPurple),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("chat_owner_button")
            ) {
                Icon(imageVector = Icons.Default.Forum, contentDescription = "chat icon")
                Spacer(modifier = Modifier.width(10.dp))
                Text("CHAT WITH ASSET OWNER", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ------ TRANSACTION LOADING OVERLAY DIALOG ------

@Composable
fun BlockchainProcessingDialog(
    status: String,
    txHash: String,
    confirmations: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberBorder(if (status.contains("ERROR")) CyberPink else CyberCyan, 16.dp)
                .background(CyberDark, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (status.contains("ERROR")) {
                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "err", tint = CyberPink, modifier = Modifier.size(56.dp))
                    Text(
                        text = "TRANSACTION REJECTED",
                        color = CyberPink,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                    Text(
                        text = status,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = CyberPink)) {
                        Text("DISMISS NODE CALL", color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                } else if (status == "SUCCESS") {
                    Icon(imageVector = Icons.Default.CheckCircleOutline, contentDescription = "success", tint = CyberGlow, modifier = Modifier.size(56.dp))
                    Text(
                        text = "LEDGER CONTRACT COMPLETED",
                        color = CyberGlow,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Ownership signature transferred and verified securely on-chain. Escrow protection successfully locked.",
                        color = SoftGrey,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    if (txHash.isNotBlank()) {
                        Text(
                            text = "HASH: ${txHash.take(20)}...",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = CyberGlow)) {
                        Text("ACKNOWLEDGEMENT SIGNED", color = CyberDark, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                } else {
                    CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(48.dp))
                    Text(
                        text = when (status) {
                            "ESTIMATING" -> "ESTIMATING GAS METRICS LIMIT..."
                            "SIGNING" -> "CRYPTOGRAPHIC CRYPTO WALLET SIGNATURE..."
                            "BROADCASTING" -> "BROADCASTING TO DECENTRALIZED MEMORY POOL..."
                            "CONFIRMING" -> "WAITING FOR BLOCK CONFIRMATIONS ($confirmations/4)..."
                            else -> "PROCESSING QUANTUM CALL..."
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "DMA smart nodes are confirming indices on the cryptographically secure blockchain network layers.",
                        color = SoftGrey,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
