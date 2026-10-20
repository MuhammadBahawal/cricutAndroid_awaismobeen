package com.cricutandroid.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cricutandroid.app.Constants
import com.cricutandroid.app.managers.BillingManager

enum class PaywallPlan {
    Weekly, Monthly, Yearly
}

/**
 * Full Paywall Screen / Dialog mirroring iOS SubscriptionVC & NewPaywallView.swift.
 */
@Composable
fun SubscriptionPaywallDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf(PaywallPlan.Monthly) }
    val context = LocalContext.current

    val primaryGreen = Color(0xFF43C485)
    val selectedBg = Color(0xFFF3FCF7)
    val neutralBorder = Color(0xFFD9D9D9)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFEEEEEE), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title Header
                    Text(
                        text = "Get Access All Features",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Without any Limitations",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryGreen
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Carousel feature box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFBFBFB))
                            .border(1.dp, Color(0xFFD8D8D8), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "1000+ Cut Files & Crafts",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                            Text(
                                text = "Generate clean, precision-ready cut files, sublimations, and 3D crafts with AI.",
                                fontSize = 13.sp,
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Plan Cards
                    // 1. Weekly
                    PlanCard(
                        title = "Weekly",
                        badge = "BASIC",
                        badgeColor = Color(0xFFECE9E9),
                        badgeTextColor = Color.Black,
                        price = "$3.49",
                        detail = "",
                        strikePrice = "$9.99",
                        isSelected = selectedPlan == PaywallPlan.Weekly,
                        onClick = { selectedPlan = PaywallPlan.Weekly }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Monthly
                    PlanCard(
                        title = "Monthly",
                        badge = "FREE TRIAL",
                        badgeColor = primaryGreen,
                        badgeTextColor = Color.White,
                        price = "$8.89",
                        detail = "$2.05/week",
                        strikePrice = null,
                        isSelected = selectedPlan == PaywallPlan.Monthly,
                        onClick = { selectedPlan = PaywallPlan.Monthly }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Yearly
                    PlanCard(
                        title = "Yearly",
                        badge = "SAVE 90%",
                        badgeColor = Color(0xFFECE9E9),
                        badgeTextColor = Color.Black,
                        price = "$49.99",
                        detail = "$4.16/month",
                        strikePrice = null,
                        isSelected = selectedPlan == PaywallPlan.Yearly,
                        onClick = { selectedPlan = PaywallPlan.Yearly }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "3 Days free trial, then $8.89 per month",
                        fontSize = 12.sp,
                        color = Color.Black.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Continue Button
                    Button(
                        onClick = {
                            BillingManager.setPremiumStatus(true)
                            onSuccess()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                    ) {
                        Text(
                            text = "CONTINUE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Auto-renewable, Cancel anytime",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Footer Links
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Terms of Use",
                            fontSize = 12.sp,
                            color = Color.Blue,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TERMS_OF_USE)))
                            }
                        )
                        Text(
                            text = "Continue Free",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.clickable { onDismiss() }
                        )
                        Text(
                            text = "Restore",
                            fontSize = 12.sp,
                            color = Color.Blue,
                            modifier = Modifier.clickable {
                                BillingManager.setPremiumStatus(true)
                                onSuccess()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }

@Composable
private fun PlanCard(
    title: String,
    badge: String,
    badgeColor: Color,
    badgeTextColor: Color,
    price: String,
    detail: String,
    strikePrice: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryGreen = Color(0xFF43C485)
    val selectedBg = Color(0xFFF3FCF7)
    val neutralBorder = Color(0xFFD9D9D9)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) selectedBg else Color.White)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) primaryGreen else neutralBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio Indicator
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) primaryGreen else Color.Transparent)
                    .border(1.5.dp, if (isSelected) primaryGreen else Color(0xFFD6D6D6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = price,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    if (strikePrice != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strikePrice,
                            fontSize = 12.sp,
                            color = Color(0xFF9A4F39),
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    if (detail.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = detail,
                            fontSize = 12.sp,
                            color = Color.Black.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor
                )
            }
        }
    }
}
