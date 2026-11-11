package com.example.firelauncher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.ui.theme.FavoriteYellow
import com.example.firelauncher.ui.theme.TextPrimary
import com.example.firelauncher.ui.theme.TextSecondary

/**
 * Hero section displaying the currently focused app's title, package/category info,
 * and context hints (e.g. "Hold OK to add to favorites").
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroSection(
    focusedApp: AppModel?,
    isFavorite: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedContent(
            targetState = focusedApp to isFavorite,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "HeroAnimation"
        ) { (app, fav) ->
            if (app != null) {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.label,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (fav) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(FavoriteYellow.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "FAVORITE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FavoriteYellow
                                )
                            }
                        } else if (app.isSystemShortcut) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SYSTEM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val hintText = when {
                        app.isSystemShortcut -> "Press OK to open settings"
                        fav -> "Hold OK for options (Remove favorite, App info / Force stop)"
                        else -> "Hold OK for options (Add to favorites, App info / Force stop)"
                    }

                    Text(
                        text = hintText,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            } else {
                Text(
                    text = "Select an app",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
        }
    }
}
