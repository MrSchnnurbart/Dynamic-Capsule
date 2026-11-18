package com.dynamiccapsule.feature.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dynamiccapsule.core.model.tr
import com.dynamiccapsule.core.system.CapsuleServiceLocator

/**
 * Guide d'accueil et de présentation interactive au premier démarrage (FR / EN) :
 * Explique aux nouveaux utilisateurs le fonctionnement de Dynamic Capsule, le calibrage
 * du Cercle Rouge de la caméra, les cartes dynamiques, les gestes et les permissions.
 */
@Composable
fun FirstLaunchWelcomeGuideScreen(
    onFinishGuide: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val generalSettings by CapsuleServiceLocator.preferencesRepository.generalSettings.collectAsState()
    val lang = generalSettings.languageCode

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 5

    var permissionSnapshot by remember {
        mutableStateOf(CapsuleServiceLocator.checkPermissions(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionSnapshot = CapsuleServiceLocator.checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Barre supérieure : Choix rapide de la langue (FR / EN) + bouton Passer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("fr" to "🇫🇷 Français", "en" to "🇬🇧 English").forEach { (code, label) ->
                        FilterChip(
                            selected = lang == code,
                            onClick = {
                                CapsuleServiceLocator.preferencesRepository.updateGeneralSettings {
                                    it.copy(languageCode = code)
                                }
                                runCatching {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(code)
                                    )
                                }
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                TextButton(onClick = onFinishGuide) {
                    Text(tr(lang, "Passer", "Skip"))
                }
            }

            // Contenu central animé selon l'étape
            AnimatedContent(
                targetState = currentStep,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "welcomeStepContent"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (step) {
                        0 -> WelcomeStepHero(
                            icon = Icons.Default.AutoAwesome,
                            badgeText = tr(lang, "BIENVENUE • OPEN SOURCE", "WELCOME • OPEN SOURCE"),
                            title = tr(
                                lang,
                                "Le Dynamic Island d'Apple sur votre Android",
                                "Apple's Dynamic Island on Your Android"
                            ),
                            subtitle = tr(
                                lang,
                                "Une capsule noire profonde (#000000), fluide à 120 fps, 100 % gratuite, open-source (Licence MIT) et sans aucune publicité.",
                                "A pure #000000 pitch-black capsule with 120 fps spring physics, 100% free, open-source (MIT License), and completely ad-free."
                            ),
                            visualContent = {
                                // Illustration visuelle de la capsule Apple scindée
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .width(210.dp)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.Black)
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF14151A))
                                        )
                                        Text("02:45", color = Color(0xFFFF9F0A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            bullets = listOf(
                                tr(
                                    lang,
                                    "Opacité à 0 % au repos : aucune barre noire inutile quand rien ne joue (protège votre écran OLED).",
                                    "0% idle opacity by default: no intrusive black bar when nothing is active (protects your OLED screen)."
                                ),
                                tr(
                                    lang,
                                    "Animations physiques naturelles (« Comme un Fruit / Ressort Apple » ou « Comme un Robot »).",
                                    "Natural spring physics ('Like a Fruit / Apple Spring' or 'Like a Robot / Smooth Android')."
                                ),
                                tr(
                                    lang,
                                    "Profils automatiques séparés pour le mode Portrait et le mode Paysage.",
                                    "Automatic independent profiles for Portrait and Landscape orientations."
                                )
                            )
                        )

                        1 -> WelcomeStepHero(
                            icon = Icons.Default.CameraAlt,
                            badgeText = tr(lang, "CALIBRAGE ANTI-MASQUAGE", "ANTI-OCCLUSION CALIBRATION"),
                            title = tr(
                                lang,
                                "Adaptez la Zone Caméra à votre téléphone",
                                "Adapt the Camera Zone to Your Phone"
                            ),
                            subtitle = tr(
                                lang,
                                "Quel que soit l'emplacement de votre caméra frontale (Gauche, Centre, Droite ou Sous la barre d'état), aucune information ne sera jamais cachée.",
                                "No matter where your front camera sits (Left, Center, Right, or Below Status Bar), no icon or text will ever be hidden."
                            ),
                            visualContent = {
                                Box(
                                    modifier = Modifier
                                        .width(230.dp)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color.White.copy(alpha = 0.45f))
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp, 26.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(Color(0xFFFF3B30))
                                                .border(1.dp, Color.White, RoundedCornerShape(50)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("CAM", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("88%", color = Color(0xFF30D158), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            bullets = listOf(
                                tr(
                                    lang,
                                    "Activez le « Cercle rouge » sur l'écran d'accueil et alignez-le directement sous votre objectif photo.",
                                    "Turn on the 'Red Circle' guide on the home tab and align it right under your physical camera lens."
                                ),
                                tr(
                                    lang,
                                    "Ajustez la largeur du poinçon caméra : les icônes et textes s'écartent automatiquement autour de cette zone.",
                                    "Adjust the camera cutout width: icons and text automatically split around this safe zone."
                                ),
                                tr(
                                    lang,
                                    "Choisissez Gauche, Centre, Droite ou Sous la barre d'état en un clic.",
                                    "Select Left, Center, Right, or Below Status Bar in a single tap."
                                )
                            )
                        )

                        2 -> WelcomeStepHero(
                            icon = Icons.Default.Layers,
                            badgeText = tr(lang, "ACTIVITÉS EN DIRECT", "LIVE ACTIVITIES"),
                            title = tr(
                                lang,
                                "7 Cartes Dynamiques & 17 Alertes Système",
                                "7 Dynamic Cards & 17 System Popups"
                            ),
                            subtitle = tr(
                                lang,
                                "Suivez vos activités en cours d'un seul coup d'œil et réorganisez leur priorité par un simple glisser-déposer.",
                                "Track your ongoing activities at a glance and reorder their display priority via drag and drop."
                            ),
                            visualContent = null,
                            bullets = listOf(
                                tr(
                                    lang,
                                    "Cartes Dynamiques : Lecteur média, Appels, Minuteur & Chronomètre, Progression (Maps, Play Store, Telegram), Lampe torche, Volume et Charge.",
                                    "Dynamic Cards: Media Player, Calls, Timer & Stopwatch, Live Progress (Maps, Play Store, Telegram), Flashlight, Volume, and Charging."
                                ),
                                tr(
                                    lang,
                                    "Capsule Scindée : lorsque 2 cartes sont actives en même temps (ex. Musique + Minuteur), une pastille secondaire apparaît à droite.",
                                    "Split Capsule: when 2 cards are active simultaneously (e.g. Music + Timer), a secondary pill appears on the right."
                                ),
                                tr(
                                    lang,
                                    "17 Popups Mini-Capsule : Déverrouillage, Batterie 5%, Wi-Fi, VPN, Bluetooth, Mode sonore, USB et Batterie AirPods (MaterialPods).",
                                    "17 Mini-Capsule Popups: Unlock, 5% Low Battery, Wi-Fi, VPN, Bluetooth, Sound Mode, USB, and AirPods battery rings."
                                )
                            )
                        )

                        3 -> WelcomeStepHero(
                            icon = Icons.Default.TouchApp,
                            badgeText = tr(lang, "CONTRÔLE TACTILE", "TOUCH CONTROLS"),
                            title = tr(
                                lang,
                                "Gestes Rapides & Notifications d'Apps",
                                "Quick Gestures & App Notifications"
                            ),
                            subtitle = tr(
                                lang,
                                "Interagissez avec la capsule sans quitter votre application en cours.",
                                "Interact with the capsule without leaving your current app."
                            ),
                            visualContent = null,
                            bullets = listOf(
                                tr(
                                    lang,
                                    "Touchez une carte compacte pour l'étendre, et balayez vers le haut (Swipe up) pour la replier.",
                                    "Tap a compact card to expand it, and swipe up to collapse it back."
                                ),
                                tr(
                                    lang,
                                    "5 gestes personnalisables (Simple, Long, Double, Balayage gauche/droite) avec 16 actions : Capture d'écran, Lampe torche, Curseurs Volume/Luminosité, QR Code, Recherche d'apps, ChatGPT, Défilement haut/bas...",
                                    "5 customizable gestures (Single, Long, Double, Swipe Left/Right) with 16 actions: Screenshot, Flashlight, Volume/Brightness Sliders, QR Code, App Search, ChatGPT, Smart Scroll..."
                                ),
                                tr(
                                    lang,
                                    "Notifications d'applications (WhatsApp, Telegram, Messages...) directement dans la capsule avec boutons de réponse.",
                                    "App notifications (WhatsApp, Telegram, Messages...) right inside the capsule with action buttons."
                                )
                            )
                        )

                        else -> {
                            // Étape 5 : Activation rapide des permissions
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tr(lang, "DERNIÈRE ÉTAPE", "FINAL STEP"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(lang, "Activez les accès système", "Enable System Permissions"),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = tr(
                                        lang,
                                        "Ces autorisations permettent d'afficher la capsule par-dessus la barre d'état et de lire votre lecteur média en cours. Aucune donnée personnelle n'est collectée.",
                                        "These permissions allow rendering the capsule above the status bar and reading active media sessions. No personal data is ever collected."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                WelcomePermissionItem(
                                    title = tr(lang, "1. Service d'Accessibilité (Overlay)", "1. Accessibility Service (Overlay)"),
                                    subtitle = tr(lang, "Pour afficher la capsule et détecter les gestes", "To draw the capsule and detect gestures"),
                                    icon = Icons.Default.AccessibilityNew,
                                    isGranted = permissionSnapshot.isAccessibilityServiceEnabled,
                                    buttonText = if (permissionSnapshot.isAccessibilityServiceEnabled) {
                                        tr(lang, "Actif", "Active")
                                    } else {
                                        tr(lang, "Activer", "Enable")
                                    },
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            )
                                        }
                                    }
                                )

                                WelcomePermissionItem(
                                    title = tr(lang, "2. Accès aux Notifications & Médias", "2. Notifications & Media Access"),
                                    subtitle = tr(lang, "Pour le lecteur musical, le minuteur et les messages", "For music player, timers, and messages"),
                                    icon = Icons.Default.NotificationsActive,
                                    isGranted = permissionSnapshot.isNotificationListenerEnabled,
                                    buttonText = if (permissionSnapshot.isNotificationListenerEnabled) {
                                        tr(lang, "Actif", "Active")
                                    } else {
                                        tr(lang, "Autoriser", "Allow")
                                    },
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            )
                                        }
                                    }
                                )

                                WelcomePermissionItem(
                                    title = tr(lang, "3. Exemption Batterie", "3. Unrestricted Battery"),
                                    subtitle = tr(lang, "Empêche la fermeture en arrière-plan", "Prevents background suspension"),
                                    icon = Icons.Default.BatteryChargingFull,
                                    isGranted = permissionSnapshot.isBatteryOptimizationIgnored,
                                    buttonText = if (permissionSnapshot.isBatteryOptimizationIgnored) {
                                        tr(lang, "Actif", "Active")
                                    } else {
                                        tr(lang, "Exempter", "Allow")
                                    },
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                    Uri.parse("package:${context.packageName}")
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Barre inférieure : Indicateurs d'étape + boutons Précédent / Suivant / Commencer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(totalSteps) { index ->
                        val selected = index == currentStep
                        Box(
                            modifier = Modifier
                                .width(if (selected) 24.dp else 8.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { currentStep = index }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Précédent", "Back"))
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps - 1) {
                                currentStep += 1
                            } else {
                                onFinishGuide()
                            }
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (currentStep < totalSteps - 1) {
                            Text(tr(lang, "Suivant", "Next"), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Commencer", "Get Started"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepHero(
    icon: ImageVector,
    badgeText: String,
    title: String,
    subtitle: String,
    visualContent: (@Composable () -> Unit)?,
    bullets: List<String>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (visualContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF222226), Color(0xFF151518))
                        )
                    )
                    .padding(vertical = 22.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                visualContent()
            }
        }

        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                bullets.forEach { line ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF30D158),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePermissionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF30D158) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}
