package com.cricutandroid.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricutandroid.app.Constants
import com.cricutandroid.app.managers.BillingManager
import com.cricutandroid.app.viewmodel.MainViewModel

/**
 * Navigation Drawer / Side Menu mirroring iOS LeftMenuVC.
 */
@Composable
fun LeftMenuDrawerContent(
    isProUser: Boolean,
    onClose: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenImportGuide: () -> Unit,
    onOpenDrafts: () -> Unit
) {
    val context = LocalContext.current
    val primaryGreen = Color(0xFF08B67D)

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(Color(0xFFF8F9FA))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Menu",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Menu",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Upgrade Banner if not Pro
        if (!isProUser) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFC72C))
                    .clickable { onOpenPaywall() }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Upgrade to Pro",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Unlock all cutfiles & tools",
                            fontSize = 12.sp,
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Projects & Account
        MenuSectionHeader(title = "My Workspace")
        MenuItemRow(
            icon = Icons.Default.Folder,
            title = "My Projects / Drafts",
            onClick = {
                onClose()
                onOpenDrafts()
            }
        )

        if (!isProUser) {
            MenuItemRow(
                icon = Icons.Default.Refresh,
                title = "Restore Purchase",
                onClick = {
                    BillingManager.setPremiumStatus(true)
                    onClose()
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Help & Feedback
        MenuSectionHeader(title = "Help & Feedback")
        MenuItemRow(
            icon = Icons.Default.Email,
            title = "Contact Us",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${Constants.EMAIL_ADDRESS}")
                }
                context.startActivity(intent)
            }
        )
        MenuItemRow(
            icon = Icons.Default.ThumbUp,
            title = "Rate Us",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                context.startActivity(intent)
            }
        )
        MenuItemRow(
            icon = Icons.Default.Share,
            title = "Share App",
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Check out Design Studio - Craft & Cutfiles for Cricut!")
                }
                context.startActivity(Intent.createChooser(intent, "Share via"))
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Import
        MenuSectionHeader(title = "Import")
        MenuItemRow(
            icon = Icons.Default.Info,
            title = "How to Import Design",
            onClick = {
                onClose()
                onOpenImportGuide()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Legal
        MenuSectionHeader(title = "Legal")
        MenuItemRow(
            icon = Icons.Default.Description,
            title = "Terms of Use",
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TERMS_OF_USE)))
            }
        )
        MenuItemRow(
            icon = Icons.Default.PrivacyTip,
            title = "Privacy Policy",
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PRIVACY_POLICY)))
            }
        )
    }
}

@Composable
private fun MenuSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF08B67D),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

/**
 * Settings tab screen mirroring iOS LeftMenuVC when hosted in HomeTabbarVC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onOpenPaywall: () -> Unit,
    onOpenDrafts: () -> Unit
) {
    val context = LocalContext.current
    var showImportGuideDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Pro Upgrade Banner if not Pro
            if (!viewModel.isProUser) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFC72C))
                        .clickable { onOpenPaywall() }
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Design Studio Pro",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Unlock all cutfiles, unlimited AI & fonts",
                                    fontSize = 12.sp,
                                    color = Color.Black.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = onOpenPaywall,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(text = "Upgrade", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // My Workspace
            MenuSectionHeader(title = "My Workspace")
            MenuItemRow(
                icon = Icons.Default.Folder,
                title = "My Projects / Saved Drafts",
                onClick = onOpenDrafts
            )

            if (!viewModel.isProUser) {
                MenuItemRow(
                    icon = Icons.Default.Refresh,
                    title = "Restore Purchase",
                    onClick = {
                        viewModel.refreshPremiumStatus()
                        Toast.makeText(context, "Purchases restored", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Help & Feedback
            MenuSectionHeader(title = "Help & Feedback")
            MenuItemRow(
                icon = Icons.Default.Email,
                title = "Contact Support",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${Constants.EMAIL_ADDRESS}")
                        putExtra(Intent.EXTRA_SUBJECT, "Design Studio Android Feedback")
                    }
                    context.startActivity(intent)
                }
            )
            MenuItemRow(
                icon = Icons.Default.ThumbUp,
                title = "Rate App on Google Play",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                    context.startActivity(intent)
                }
            )
            MenuItemRow(
                icon = Icons.Default.Share,
                title = "Share Design Studio",
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Design Studio for Cricut - Create cutfiles, stickers & crafts!\nhttps://play.google.com/store/apps/details?id=${context.packageName}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share via"))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Guides
            MenuSectionHeader(title = "How-To Guides")
            MenuItemRow(
                icon = Icons.Default.Info,
                title = "How to Import SVGs into Cricut Machine",
                onClick = { showImportGuideDialog = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Legal
            MenuSectionHeader(title = "Legal")
            MenuItemRow(
                icon = Icons.Default.Description,
                title = "Terms of Use",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TERMS_OF_USE)))
                }
            )
            MenuItemRow(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PRIVACY_POLICY)))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Version 1.0 (Build 1)",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    if (showImportGuideDialog) {
        AlertDialog(
            onDismissRequest = { showImportGuideDialog = false },
            title = { Text(text = "How to Import SVGs", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "1. Export your cutfile as an SVG using the Canvas Editor or Save screen.\n\n" +
                            "2. Open Cricut Design Space on your mobile or desktop.\n\n" +
                            "3. Select 'Upload' > 'Upload Image' > Browse to your exported SVG.\n\n" +
                            "4. Name your design and add it to your Cricut Canvas ready to Cut or Print then Cut!",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showImportGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF08B67D))
                ) {
                    Text("Got it")
                }
            }
        )
    }
}
