package com.storage.intelligence.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storage.intelligence.core.ui.theme.EmeraldPrimary
import com.storage.intelligence.core.ui.theme.PrimaryCyan
import com.storage.intelligence.core.ui.theme.SecondaryEmerald
import kotlinx.coroutines.delay

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow

data class WorldFact(
    val category: String,
    val emoji: String,
    val fact: String,
    val location: String
)

val CURATED_WORLD_FACTS = listOf(
    WorldFact(
        category = "Living Planet",
        emoji = "🌳",
        fact = "There are approximately 3 trillion trees on planet Earth — far more than the estimated 100 billion stars in our entire Milky Way galaxy.",
        location = "Global Forests"
    ),
    WorldFact(
        category = "Deep Oceans",
        emoji = "🌊",
        fact = "The Mariana Trench is so deep (nearly 11,000 meters) that if Mount Everest were dropped into it, its summit would still be submerged under 2 kilometers of water.",
        location = "Pacific Ocean"
    ),
    WorldFact(
        category = "Timeless Nature",
        emoji = "🍯",
        fact = "Honey never spoils. Archaeologists have excavated 3,000-year-old pots of honey in ancient Egyptian tombs that remain completely preserved and edible.",
        location = "Ancient Egypt"
    ),
    WorldFact(
        category = "Gentle Giants",
        emoji = "🐋",
        fact = "A blue whale's heart is the size of a small car and weighs over 400 pounds. Its rhythmic pulse can be detected underwater from more than 2 miles away.",
        location = "Open Oceans"
    ),
    WorldFact(
        category = "Earth's Atmosphere",
        emoji = "⚡",
        fact = "A single bolt of lightning superheats the surrounding air to 30,000 Kelvin (over 53,000°F) — nearly five times hotter than the surface of the Sun.",
        location = "Global Skies"
    ),
    WorldFact(
        category = "Ancient Architecture",
        emoji = "🏛️",
        fact = "The Great Pyramid of Giza was the tallest man-made structure in the world for over 3,800 years until England's Lincoln Cathedral was finished in 1311.",
        location = "Giza, Egypt"
    ),
    WorldFact(
        category = "Tectonic Wonder",
        emoji = "🏔️",
        fact = "Mount Everest grows about 4 millimeters taller every year as the Indian continental plate continues to collide into the Eurasian plate.",
        location = "Himalayas"
    ),
    WorldFact(
        category = "Animal Language",
        emoji = "🐬",
        fact = "Bottlenose dolphins use individual acoustic signature whistles as personal names, which friends in their pod use to call and locate each other.",
        location = "Coastal Waters"
    ),
    WorldFact(
        category = "Celestial Rhythm",
        emoji = "🌌",
        fact = "A single day on Venus lasts longer than a Venusian year: it takes 243 Earth days to rotate once on its axis, but only 225 Earth days to orbit the Sun.",
        location = "Solar System"
    ),
    WorldFact(
        category = "Animal Romance",
        emoji = "🐧",
        fact = "Gentoo penguins propose to their lifelong partners by scouring entire coastlines to find the smoothest, most flawless pebble on the beach.",
        location = "Antarctica"
    ),
    WorldFact(
        category = "Ancient Forest",
        emoji = "🌲",
        fact = "The oldest known living tree on Earth is 'Methuselah', a Great Basin bristlecone pine in California that has been growing for more than 4,850 years.",
        location = "California, USA"
    ),
    WorldFact(
        category = "Seasonal Wonder",
        emoji = "🌸",
        fact = "In Japan, meteorological teams track the 'Sakura Zensen' (cherry blossom front) with scientific precision as blooming sweeps northward across the islands.",
        location = "Japan"
    ),
    WorldFact(
        category = "Desert Secrets",
        emoji = "🏜️",
        fact = "Beneath the shifting dunes of the Sahara Desert lies the Nubian Sandstone Aquifer, holding over 150,000 cubic kilometers of ancient fossil water.",
        location = "Sahara, Africa"
    ),
    WorldFact(
        category = "Avian Elegance",
        emoji = "🦩",
        fact = "A flock of flamingos is formally known as a 'flamboyance'. Their vibrant pink feathers come naturally from carotenoid pigments in the microscopic algae they eat.",
        location = "Tropical Lagoons"
    ),
    WorldFact(
        category = "Atmospheric Light",
        emoji = "🌈",
        fact = "Rainbows are actually complete 360-degree circles. We usually only see half of the circle from the ground because the horizon line cuts off the lower arc.",
        location = "Earth's Atmosphere"
    ),
    WorldFact(
        category = "Cosmic Dust",
        emoji = "✨",
        fact = "Every day, approximately 100 tons of cosmic dust and micrometeorites drift harmlessly through the atmosphere and settle gently across Earth's surface.",
        location = "Upper Atmosphere"
    ),
    WorldFact(
        category = "Autumn Chemistry",
        emoji = "🍁",
        fact = "Leaves don't turn red and gold in autumn — those stunning pigments were hidden inside the leaf all summer, only revealed when green chlorophyll breaks down.",
        location = "Temperate Forests"
    ),
    WorldFact(
        category = "Subterranean World",
        emoji = "🏞️",
        fact = "The Puerto Princesa Subterranean River in the Philippines flows for over 8 kilometers inside limestone caves directly into the open ocean.",
        location = "Palawan, Philippines"
    ),
    WorldFact(
        category = "Healing Flora",
        emoji = "🌻",
        fact = "Sunflowers are hyperaccumulators capable of drawing heavy metals and radioactive contaminants from the soil, naturally decontaminating the environment.",
        location = "Agricultural Fields"
    ),
    WorldFact(
        category = "Incredible Migration",
        emoji = "🦋",
        fact = "Monarch butterflies navigate across 3,000 miles during migration by reading polarized sunlight patterns and Earth's subtle geomagnetic field.",
        location = "North America"
    ),
    WorldFact(
        category = "Underground Labyrinth",
        emoji = "🦇",
        fact = "Mammoth Cave in Kentucky is the world's longest known cave system, with more than 420 miles of surveyed underground passageways and chambers.",
        location = "Kentucky, USA"
    ),
    WorldFact(
        category = "Marine Biodiversity",
        emoji = "🐠",
        fact = "Australia's Great Barrier Reef is so vast that it can be seen from Earth orbit — spanning over 2,300 kilometers with 900 individual islands.",
        location = "Coral Sea"
    ),
    WorldFact(
        category = "Volcanic Birth",
        emoji = "🌋",
        fact = "In November 1963, a volcanic eruption in the North Atlantic gave birth to a brand new island named Surtsey, which is now a pristine UNESCO World Heritage reserve.",
        location = "Iceland"
    ),
    WorldFact(
        category = "Atmospheric Aurora",
        emoji = "🌌",
        fact = "The Northern and Southern Lights (Auroras) occur when charged solar wind particles crash into Earth's magnetic shield, exciting oxygen and nitrogen atoms.",
        location = "Polar Regions"
    )
)

/**
 * An engaging, visually rich loading screen that replaces empty spinners with fascinating
 * world facts, smooth animations, and real-time status updates.
 * Optimized for lightweight execution on low-memory (4GB RAM) devices.
 */
@Composable
fun WorldFactsLoadingView(
    modifier: Modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp),
    statusTitle: String = "Analyzing storage partitions…",
    statusSubtitle: String? = "Reading filesystem and media index",
    accentColor: Color = EmeraldPrimary
) {
    var factIndex by remember { mutableIntStateOf(0) }
    val currentFact = remember(factIndex) {
        CURATED_WORLD_FACTS[factIndex % CURATED_WORLD_FACTS.size]
    }

    // Auto-advance fact every 4 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L)
            factIndex = (factIndex + 1) % CURATED_WORLD_FACTS.size
        }
    }

    // Continuous smooth animation using Draw-phase graphicsLayer (Zero Recomposition overhead!)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rotationState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScaleState = infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Cache Brushes so zero objects are allocated during draws/recompositions
    val sweepBrush = remember(accentColor) {
        Brush.sweepGradient(
            listOf(
                Color.Transparent,
                accentColor.copy(alpha = 0.2f),
                accentColor,
                PrimaryCyan
            )
        )
    }

    val radialBrush = remember(accentColor) {
        Brush.radialGradient(
            listOf(
                accentColor.copy(alpha = 0.9f),
                SecondaryEmerald.copy(alpha = 0.3f),
                Color.Transparent
            )
        )
    }

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val cardBorderBrush = remember(accentColor, surfaceVariantColor) {
        Brush.verticalGradient(
            listOf(
                accentColor.copy(alpha = 0.35f),
                surfaceVariantColor.copy(alpha = 0.2f)
            )
        )
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // ── TOP: GLOWING RADAR PROGRESS RING (Draw-phase animated, zero recomposition) ──
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background soft glow
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f))
                )

                // Rotating gradient outer ring (Rotated on GPU in Draw phase!)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            rotationZ = rotationState.value
                        }
                        .border(
                            width = 2.5.dp,
                            brush = sweepBrush,
                            shape = CircleShape
                        )
                )

                // Inner core pulsing orb (Scaled on GPU in Draw phase!)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer {
                            scaleX = pulseScaleState.value
                            scaleY = pulseScaleState.value
                        }
                        .clip(CircleShape)
                        .background(radialBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── STATUS TITLE & SUBTITLE ──────────────────────────────────────────────
            Text(
                text = statusTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (!statusSubtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── DID YOU KNOW? WORLD FACTS CARD (Interactive & Animated) ─────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        // User can tap card to instantly skip to the next fact
                        factIndex = (factIndex + 1) % CURATED_WORLD_FACTS.size
                    }
                    .border(
                        1.dp,
                        cardBorderBrush,
                        RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Header Row: Category Badge + "Next →" hint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DID YOU KNOW?",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentColor,
                                        letterSpacing = 0.8.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•  ${currentFact.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Tap for next →",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Animated Fact Content
                    AnimatedContent(
                        targetState = currentFact,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300)) +
                                    slideInVertically(animationSpec = tween(300)) { height -> height / 2 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(200)) +
                                            slideOutVertically(animationSpec = tween(200)) { height -> -height / 2 }
                                )
                        },
                        label = "factTransition"
                    ) { fact ->
                        Column {
                            Row(
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = fact.emoji,
                                    fontSize = 26.sp,
                                    modifier = Modifier.padding(end = 10.dp, top = 2.dp)
                                )

                                Text(
                                    text = fact.fact,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📍 ${fact.location}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
