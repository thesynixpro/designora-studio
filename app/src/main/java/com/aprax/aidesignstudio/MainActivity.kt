package com.aprax.aidesignstudio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aprax.aidesignstudio.ai.DesignAiClient
import com.aprax.aidesignstudio.ai.ProviderSettings
import com.aprax.aidesignstudio.data.DesignoraDatabase
import com.aprax.aidesignstudio.data.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Ink = Color(0xFF222622)
private val Muted = Color(0xFF777D76)
private val Paper = Color(0xFFF6F5F1)
private val Accent = Color(0xFF5D55B8)
private val Line = Color(0xFFE6E4DD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = DesignoraDatabase.get(this)
        val settings = ProviderSettings(this)
        setContent { DesignoraApp(db, settings) }
    }
}

@Composable
private fun DesignoraApp(db: DesignoraDatabase, settings: ProviderSettings) {
    val nav = rememberNavController()
    val projects by db.projects().observeAll().collectAsState(initial = emptyList())
    val dark = remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = if (dark.value) darkColorScheme(primary = Color(0xFFB9B1FF), background = Color(0xFF171916), surface = Color(0xFF222520)) else lightColorScheme(primary = Accent, background = Paper, surface = Color.White, onBackground = Ink, onSurface = Ink)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            NavHost(navController = nav, startDestination = "home") {
                composable("home") { HomeScreen(projects, onCreate = { nav.navigate("create") }, onProjects = { nav.navigate("projects") }, onTemplates = { nav.navigate("templates") }, onSettings = { nav.navigate("settings") }, onOpen = { nav.navigate("editor/$it") }, onTheme = { dark.value = !dark.value }) }
                composable("create") { CreateScreen(settings, onBack = { nav.popBackStack() }, onCreated = { nav.navigate("editor/$it") { popUpTo("home") } }) }
                composable("projects") { ProjectsScreen(projects, db, onBack = { nav.popBackStack() }, onOpen = { nav.navigate("editor/$it") }) }
                composable("templates") { TemplatesScreen(db, onBack = { nav.popBackStack() }, onOpen = { nav.navigate("editor/$it") }) }
                composable("settings") { SettingsScreen(settings, onBack = { nav.popBackStack() }) }
                composable("editor/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    EditorScreen(id, db, onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun ScreenFrame(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Muted)
            }
            Text("D", Modifier.size(34.dp).clip(CircleShape).background(Accent).wrapContentSize(Alignment.Center), color = Color.White, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun HomeScreen(projects: List<ProjectEntity>, onCreate: () -> Unit, onProjects: () -> Unit, onTemplates: () -> Unit, onSettings: () -> Unit, onOpen: (String) -> Unit, onTheme: () -> Unit) {
    ScreenFrame("Designora Studio", "A thoughtful space for digital product design") {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFFEAE8E0)).padding(20.dp)) {
                    Text("YOUR CREATIVE WORKSPACE", fontSize = 10.sp, letterSpacing = 1.4.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text("What are we\ncreating today?", fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("Turn a clear idea into a design you can shape, refine and keep.", color = Color(0xFF62675F), fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = onCreate, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                        Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Create a design")
                    }
                }
            }
            item { SectionTitle("Start with a canvas", "Choose a direction") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        QuickTile("Mobile app", Icons.Outlined.PhoneAndroid, Modifier.weight(1f), onCreate)
                        QuickTile("Website", Icons.Outlined.Language, Modifier.weight(1f), onCreate)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        QuickTile("Landing page", Icons.Outlined.Web, Modifier.weight(1f), onCreate)
                        QuickTile("Dashboard", Icons.Outlined.Dashboard, Modifier.weight(1f), onCreate)
                    }
                }
            }
            item { Row(verticalAlignment = Alignment.CenterVertically) { SectionTitle("Recent projects", "Your work, saved on this device"); Spacer(Modifier.weight(1f)); TextButton(onClick = onProjects) { Text("See all") } } }
            if (projects.isEmpty()) item { EmptyState("No projects yet", "Create your first design or start from a built-in template.", onCreate) }
            items(projects.take(4), key = { it.id }) { project -> ProjectCard(project, onClick = { onOpen(project.id) }) }
            item { SectionTitle("Explore", "Tools for your next idea") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onTemplates, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Widgets, null); Spacer(Modifier.width(6.dp)); Text("Templates") }
                    OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Tune, null); Spacer(Modifier.width(6.dp)); Text("AI settings") }
                }
                TextButton(onClick = onTheme) { Icon(Icons.Outlined.DarkMode, null); Spacer(Modifier.width(6.dp)); Text("Switch appearance") }
            }
        }
    }
}

@Composable private fun SectionTitle(title: String, subtitle: String) { Column { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 12.sp, color = Muted) } }

@Composable
private fun QuickTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Accent); Spacer(Modifier.width(9.dp)); Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ProjectCard(project: ProjectEntity, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFEAE8F7)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.fillMaxWidth(0.65f).height(5.dp).clip(CircleShape).background(Accent))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(0xFFB8B2D9)))
                Box(Modifier.fillMaxWidth(0.8f).height(4.dp).clip(CircleShape).background(Color(0xFFB8B2D9)))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) { Text(project.name, fontWeight = FontWeight.SemiBold); Text(project.category, color = Muted, fontSize = 12.sp) }
            if (project.favorite) Icon(Icons.Outlined.Star, null, tint = Accent)
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable private fun EmptyState(title: String, message: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.DashboardCustomize, null, tint = Muted, modifier = Modifier.size(34.dp)); Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.SemiBold); Text(message, color = Muted, fontSize = 12.sp); TextButton(onClick = onAction) { Text("Get started") }
    }
}

@Composable
private fun CreateScreen(settings: ProviderSettings, onBack: () -> Unit, onCreated: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("Untitled concept") }
    var prompt by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Mobile App Design") }
    var style by remember { mutableStateOf("Modern minimal") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    ScreenFrame("New design", "Describe the product you want to make", onBack) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Project name") }, singleLine = true)
            Text("DESIGN TYPE", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(category, Modifier.weight(1f)); Icon(Icons.Outlined.ExpandMore, null) }
                DropdownMenu(expanded, { expanded = false }) { listOf("Mobile App Design", "Website Design", "Landing Page", "Dashboard", "E-commerce", "Portfolio", "Custom Design").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { category = it; expanded = false }) } }
            }
            Text("VISUAL DIRECTION", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold)
            var styleExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { styleExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(style, Modifier.weight(1f)); Icon(Icons.Outlined.ExpandMore, null) }
                DropdownMenu(styleExpanded, { styleExpanded = false }) { listOf("Modern minimal", "Editorial", "Corporate", "Luxury", "Material 3", "iOS inspired", "Brutalist").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { style = it; styleExpanded = false }) } }
            }
            OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth().weight(1f, fill = false), label = { Text("Describe your idea") }, placeholder = { Text("A calm personal finance app with a clear balance overview, spending insights, and simple navigation...") }, minLines = 5)
            Text("A detailed brief produces a more useful first draft. Your result will be saved as editable design data.", color = Muted, fontSize = 12.sp)
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            Button(onClick = {
                if (name.isBlank() || prompt.isBlank()) { error = "Enter a project name and describe your idea."; return@Button }
                loading = true; error = null
                scope.launch {
                    try {
                        val json = withContext(Dispatchers.IO) { DesignAiClient(settings).generateDesign(prompt, category, style) }
                        val id = UUID.randomUUID().toString()
                        DesignoraDatabase.get(context).projects().upsert(ProjectEntity(id, name.trim(), category, prompt.trim(), json))
                        onCreated(id)
                    } catch (e: Exception) { error = e.message ?: "Generation failed. Check your connection and API settings." }
                    finally { loading = false }
                }
            }, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                if (loading) { CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp); Spacer(Modifier.width(9.dp)); Text("Creating design…") } else { Icon(Icons.Outlined.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Generate editable design") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProjectsScreen(projects: List<ProjectEntity>, db: DesignoraDatabase, onBack: () -> Unit, onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    ScreenFrame("Projects", "Saved locally on this device", onBack) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search projects") }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
            Spacer(Modifier.height(12.dp))
            val filtered = projects.filter { it.name.contains(query, true) || it.category.contains(query, true) }
            if (filtered.isEmpty()) EmptyState("Nothing here yet", "Your created designs will appear here.", onBack)
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(filtered, key = { it.id }) { project ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { ProjectCard(project) { onOpen(project.id) } }
                        Column {
                            IconButton(onClick = { scope.launch { db.projects().setFavorite(project.id, !project.favorite) } }) { Icon(if (project.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null, tint = Accent) }
                            IconButton(onClick = { scope.launch { db.projects().delete(project.id) } }) { Icon(Icons.Outlined.DeleteOutline, null, tint = Muted) }
                        }
                    }
                }
            }
        }
    }
}

private data class BuiltInTemplate(val name: String, val category: String, val description: String, val accent: String, val title: String)
private val builtInTemplates = listOf(
    BuiltInTemplate("Fintech overview", "Mobile App Design", "A clear finance home screen with balance and spending", "#5D55B8", "Your money, in focus"),
    BuiltInTemplate("SaaS analytics", "Dashboard", "A compact analytics workspace for teams", "#286C63", "Good morning, team"),
    BuiltInTemplate("Studio landing page", "Landing Page", "Editorial hero, services and a confident call to action", "#A45B3E", "Make your next move"),
    BuiltInTemplate("Commerce storefront", "E-commerce", "A clean product discovery and shopping experience", "#B06D39", "Objects for everyday living")
)

@Composable
private fun TemplatesScreen(db: DesignoraDatabase, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    ScreenFrame("Template library", "Built-in starting points · not community submissions", onBack) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(builtInTemplates) { template ->
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                    Column(Modifier.padding(15.dp)) {
                        Box(Modifier.fillMaxWidth().height(108.dp).clip(RoundedCornerShape(10.dp)).background(Color(android.graphics.Color.parseColor(template.accent))).padding(16.dp), contentAlignment = Alignment.BottomStart) {
                            Column { Text(template.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp); Text(template.category, color = Color.White.copy(alpha = .78f), fontSize = 11.sp) }
                        }
                        Spacer(Modifier.height(10.dp)); Text(template.name, fontWeight = FontWeight.Bold); Text(template.description, color = Muted, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Button(onClick = {
                            scope.launch {
                                val id = UUID.randomUUID().toString()
                                val json = sampleDesign(template)
                                db.projects().upsert(ProjectEntity(id, template.name, template.category, "Built-in template", json))
                                onOpen(id)
                            }
                        }) { Text("Use template") } }
                    }
                }
            }
        }
    }
}

private fun sampleDesign(t: BuiltInTemplate): String {
    val color = t.accent
    val root = JSONObject().put("title", t.name).put("category", t.category)
        .put("theme", JSONObject().put("background", "#F7F6F2").put("surface", "#FFFFFF").put("text", "#222622").put("muted", "#777D76").put("accent", color))
    val elements = JSONArray()
    fun el(id: String, type: String, text: String, y: Int, h: Int, size: Int, bg: String = "#FFFFFF") {
        elements.put(JSONObject().put("id", id).put("type", type).put("text", text).put("x", 20).put("y", y).put("width", 350).put("height", h).put("fontSize", size).put("color", "#222622").put("background", bg))
    }
    el("e1", "nav", t.name, 18, 48, 16); el("e2", "heading", t.title, 88, 90, 29, "#F0EEE8"); el("e3", "text", t.description, 194, 56, 15); el("e4", "button", "Explore now", 270, 50, 15, color); el("e5", "heading", "Designed around what matters", 350, 64, 22); el("e6", "card", "A thoughtful overview\nSimple, useful information at a glance.", 430, 120, 16)
    root.put("pages", JSONArray().put(JSONObject().put("name", "Home").put("width", 390).put("height", 844).put("elements", elements)))
    return root.toString()
}

@Composable
private fun SettingsScreen(settings: ProviderSettings, onBack: () -> Unit) {
    var provider by remember { mutableStateOf(settings.provider) }
    var gemini by remember { mutableStateOf(settings.geminiKey) }
    var openRouter by remember { mutableStateOf(settings.openRouterKey) }
    var model by remember { mutableStateOf(settings.model) }
    var models by remember { mutableStateOf(emptyList<String>()) }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ScreenFrame("AI providers", "API credentials are encrypted on this device", onBack) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Provider: $provider", Modifier.weight(1f)); Icon(Icons.Outlined.ExpandMore, null) }
                DropdownMenu(expanded, { expanded = false }) { listOf("Gemini", "OpenRouter").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { provider = it; settings.provider = it; expanded = false; models = emptyList() }) } }
            }
            if (provider == "Gemini") {
                OutlinedTextField(gemini, { gemini = it }, Modifier.fillMaxWidth(), label = { Text("Gemini API key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            } else {
                OutlinedTextField(openRouter, { openRouter = it }, Modifier.fillMaxWidth(), label = { Text("OpenRouter API key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            }
            OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Model ID") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    settings.provider = provider; settings.geminiKey = gemini; settings.openRouterKey = openRouter; settings.model = model
                    loading = true; status = ""
                    scope.launch { try { models = withContext(Dispatchers.IO) { DesignAiClient(settings).fetchModels() }; status = "Fetched ${models.size} live models." } catch (e: Exception) { status = e.message ?: "Could not fetch models." } finally { loading = false } }
                }, enabled = !loading, modifier = Modifier.weight(1f)) { if (loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.CloudDownload, null); Spacer(Modifier.width(6.dp)); Text("Fetch models") }
                Button(onClick = {
                    settings.provider = provider; settings.geminiKey = gemini; settings.openRouterKey = openRouter; settings.model = model
                    loading = true
                    scope.launch { try { status = withContext(Dispatchers.IO) { DesignAiClient(settings).testConnection() } } catch (e: Exception) { status = e.message ?: "Connection failed." } finally { loading = false } }
                }, enabled = !loading, modifier = Modifier.weight(1f)) { Text("Test & save") }
            }
            if (status.isNotBlank()) Text(status, color = if (status.startsWith("Connected")) Color(0xFF26734D) else MaterialTheme.colorScheme.error, fontSize = 12.sp)
            if (models.isNotEmpty()) {
                Text("AVAILABLE MODELS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Muted)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(models.take(150)) { item -> ListItem(headlineContent = { Text(item, fontSize = 12.sp) }, trailingContent = { if (item == model) Icon(Icons.Outlined.Check, null, tint = Accent) }, modifier = Modifier.clickable { model = item; settings.model = item }) }
                }
            } else {
                Text("Gemini and OpenRouter model lists are fetched from their real APIs. No demo model list is used. Keep your API key private.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EditorScreen(id: String, db: DesignoraDatabase, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var project by remember { mutableStateOf<ProjectEntity?>(null) }
    var design by remember { mutableStateOf(JSONObject()) }
    var pageIndex by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri: Uri? ->
        if (uri != null) scope.launch { try { context.contentResolver.openOutputStream(uri)?.use { it.write(exportHtml(design).toByteArray()) }; status = "HTML export saved." } catch (e: Exception) { status = "Export failed: ${e.message}" } }
    }
    LaunchedEffect(id) {
        project = withContext(Dispatchers.IO) { db.projects().get(id) }
        design = try { JSONObject(project?.designJson ?: "{}") } catch (_: Exception) { JSONObject() }
        ready = true
    }
    if (!ready) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val pages = design.optJSONArray("pages") ?: JSONArray()
    if (pages.length() == 0) { Text("This project has no pages."); return }
    val page = pages.optJSONObject(pageIndex.coerceIn(0, pages.length() - 1)) ?: JSONObject()
    val elements = page.optJSONArray("elements") ?: JSONArray()
    val selectedElement = elements.optJSONObject(selected.coerceIn(0, (elements.length() - 1).coerceAtLeast(0)))
    fun persist() { val p = project ?: return; scope.launch { db.projects().upsert(p.copy(designJson = design.toString(), updatedAt = System.currentTimeMillis())) } }
    ScreenFrame(project?.name ?: "Editor", "Editable canvas · ${page.optString("name", "Page")}", onBack) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Page", color = Muted, fontSize = 12.sp); Spacer(Modifier.width(8.dp))
                var pageMenu by remember { mutableStateOf(false) }
                Box { OutlinedButton(onClick = { pageMenu = true }) { Text(page.optString("name", "Page 1")); Icon(Icons.Outlined.ExpandMore, null) }; DropdownMenu(pageMenu, { pageMenu = false }) { for (i in 0 until pages.length()) DropdownMenuItem(text = { Text(pages.optJSONObject(i)?.optString("name", "Page ${i+1}") ?: "Page") }, onClick = { pageIndex = i; selected = 0; pageMenu = false }) } }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { exportLauncher.launch("${project?.name?.replace(" ", "-") ?: "design"}.html") }) { Icon(Icons.Outlined.FileDownload, contentDescription = "Export HTML") }
                IconButton(onClick = { persist(); status = "Saved" }) { Icon(Icons.Outlined.Save, contentDescription = "Save") }
            }
            Surface(Modifier.weight(1f).fillMaxWidth(), color = Color(0xFFEAE9E4), shape = RoundedCornerShape(14.dp)) {
                LazyColumn(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                    item {
                        Text("${page.optInt("width", 390)} × ${page.optInt("height", 844)}", color = Muted, fontSize = 10.sp)
                    }
                    items(elements.length()) { index ->
                        val element = elements.optJSONObject(index) ?: JSONObject()
                        val bg = parseColor(element.optString("background", "#FFFFFF"), Color.White)
                        val fg = parseColor(element.optString("color", "#222622"), Ink)
                        Surface(Modifier.fillMaxWidth().then(if (index == selected) Modifier.border(2.dp, Accent, RoundedCornerShape(8.dp)) else Modifier).clickable { selected = index }, color = bg, shape = RoundedCornerShape(8.dp), shadowElevation = 1.dp) {
                            Box(Modifier.padding(14.dp).heightIn(min = 34.dp), contentAlignment = Alignment.CenterStart) {
                                Text(element.optString("text"), color = fg, fontSize = element.optInt("fontSize", 15).coerceIn(10, 34).sp, fontWeight = if (element.optString("type") == "heading") FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
            Text("SELECTED ELEMENT", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold)
            if (selectedElement != null) {
                OutlinedTextField(selectedElement.optString("text"), { value -> selectedElement.put("text", value); design = JSONObject(design.toString()); persist() }, Modifier.fillMaxWidth(), label = { Text("Content") }, minLines = 1, maxLines = 3)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Text size", fontSize = 12.sp, color = Muted); Slider(selectedElement.optInt("fontSize", 16).toFloat(), { value -> selectedElement.put("fontSize", value.toInt()); design = JSONObject(design.toString()); persist() }, Modifier.weight(1f), valueRange = 10f..34f); Text("${selectedElement.optInt("fontSize", 16)}", fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { val copy = JSONObject(selectedElement.toString()).put("id", UUID.randomUUID().toString()); elements.put(copy); design = JSONObject(design.toString()); selected = elements.length()-1; persist() }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.ContentCopy, null); Text("Duplicate") }
                    OutlinedButton(onClick = { if (elements.length() > 1) { val next = JSONArray(); for (i in 0 until elements.length()) if (i != selected) next.put(elements.get(i)); page.put("elements", next); design = JSONObject(design.toString()); selected = 0; persist() } }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.DeleteOutline, null); Text("Delete") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { val e = JSONObject().put("id", UUID.randomUUID().toString()).put("type", "text").put("text", "New text element").put("x", 20).put("y", 520).put("width", 350).put("height", 52).put("fontSize", 16).put("color", "#222622").put("background", "#FFFFFF"); elements.put(e); design = JSONObject(design.toString()); selected = elements.length()-1; persist() }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Add, null); Text("Add text") }
                Button(onClick = { persist(); status = "Saved to this device" }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Save, null); Text("Save changes") }
            }
            if (status.isNotBlank()) Text(status, color = Muted, fontSize = 11.sp)
        }
    }
}

private fun parseColor(value: String, fallback: Color): Color = try { Color(android.graphics.Color.parseColor(value)) } catch (_: Exception) { fallback }

private fun exportHtml(design: JSONObject): String {
    val page = design.optJSONArray("pages")?.optJSONObject(0) ?: JSONObject()
    val elements = page.optJSONArray("elements") ?: JSONArray()
    val background = design.optJSONObject("theme")?.optString("background", "#F7F6F2") ?: "#F7F6F2"
    val body = StringBuilder()
    for (i in 0 until elements.length()) {
        val e = elements.optJSONObject(i) ?: continue
        val text = e.optString("text").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
        val type = e.optString("type")
        val tag = if (type == "heading") "h2" else if (type == "button") "a" else "div"
        val color = e.optString("color", "#222622"); val bg = e.optString("background", "transparent"); val size = e.optInt("fontSize", 16)
        body.append("<$tag style=\"box-sizing:border-box;margin:12px 0;padding:14px;color:$color;background:$bg;font-size:${size}px;border-radius:10px;\">$text</$tag>\n")
    }
    return "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>${design.optString("title", "Designora export")}</title><style>body{font-family:Inter,Arial,sans-serif;max-width:760px;margin:32px auto;padding:20px;background:$background}</style></head><body>${body}</body></html>"
}
