package com.craftcv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcv.app.data.models.EducationEntry
import com.craftcv.app.data.models.ExperienceEntry
import com.craftcv.app.data.models.TailorResponse
import com.craftcv.app.ui.theme.CraftColors
import com.craftcv.app.ui.theme.InterFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeEditorBottomSheet(
    section: String,
    draftData: TailorResponse?,
    onDismiss: () -> Unit,
    onSave: (TailorResponse) -> Unit
) {
    if (draftData == null) {
        onDismiss()
        return
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CraftColors.Background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CraftColors.Border) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Edit ${section.replaceFirstChar { it.uppercase() }}",
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CraftColors.InkPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when (section.lowercase()) {
                "contact" -> ContactEditor(draftData.contactInfo, onSave = { updated ->
                    onSave(draftData.copy(contactInfo = updated))
                    onDismiss()
                })
                "summary" -> SummaryEditor(draftData.professionalSummary, onSave = { updated ->
                    onSave(draftData.copy(professionalSummary = updated))
                    onDismiss()
                })
                "experience" -> ExperienceEditor(draftData.experience, onSave = { updated ->
                    onSave(draftData.copy(experience = updated))
                    onDismiss()
                })
                "education" -> EducationEditor(draftData.education, onSave = { updated ->
                    onSave(draftData.copy(education = updated))
                    onDismiss()
                })
                "skills" -> SkillsEditor(draftData.skills, onSave = { updated ->
                    onSave(draftData.copy(skills = updated))
                    onDismiss()
                })
                else -> {
                    Text("Unknown section: $section")
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp)) // padding for bottom nav/keyboard
        }
    }
}

@Composable
private fun ContactEditor(
    contact: com.craftcv.app.data.models.ContactInfo,
    onSave: (com.craftcv.app.data.models.ContactInfo) -> Unit
) {
    var name by remember { mutableStateOf(contact.fullName) }
    var email by remember { mutableStateOf(contact.email) }
    var phone by remember { mutableStateOf(contact.phone) }
    var title by remember { mutableStateOf(contact.currentTitle) }
    var location by remember { mutableStateOf(contact.location) }
    var linkedin by remember { mutableStateOf(contact.linkedinUrl) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CraftTextField(value = name, onValueChange = { name = it }, label = "Full Name", placeholder = "")
        CraftTextField(value = title, onValueChange = { title = it }, label = "Professional Title", placeholder = "")
        CraftTextField(value = email, onValueChange = { email = it }, label = "Email Address", placeholder = "")
        CraftTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", placeholder = "")
        CraftTextField(value = location, onValueChange = { location = it }, label = "Location", placeholder = "")
        CraftTextField(value = linkedin, onValueChange = { linkedin = it }, label = "LinkedIn URL", placeholder = "")

        Spacer(modifier = Modifier.height(8.dp))
        CraftAccentButton(text = "Save Contact Info", onClick = {
            onSave(contact.copy(fullName=name, email=email, phone=phone, currentTitle=title, location=location, linkedinUrl=linkedin))
        }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SummaryEditor(
    summary: String,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(summary) }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFamily),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CraftColors.Accent,
                unfocusedBorderColor = CraftColors.Border
            ),
            label = { Text("Professional Summary") }
        )

        CraftAccentButton(text = "Save Summary", onClick = { onSave(text) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ExperienceEditor(
    experience: List<ExperienceEntry>,
    onSave: (List<ExperienceEntry>) -> Unit
) {
    var editList by remember { mutableStateOf(experience) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Edit job titles, companies, and dates. Individual bullet editing is in the 'Bullets' tab.", fontSize = 12.sp, color = CraftColors.InkTertiary)
        
        editList.forEachIndexed { i, entry ->
            CraftCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Role ${i + 1}", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CraftColors.InkPrimary, modifier = Modifier.weight(1f))
                        if (editList.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CraftColors.ErrorSoft,
                                modifier = Modifier.clickable { editList = editList.toMutableList().apply { removeAt(i) } },
                            ) {
                                Text("Remove", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CraftColors.Error)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = entry.jobTitle,
                        onValueChange = { newVal -> editList = editList.toMutableList().apply { this[i] = entry.copy(jobTitle = newVal) } },
                        label = { Text("Job Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = entry.company,
                        onValueChange = { newVal -> editList = editList.toMutableList().apply { this[i] = entry.copy(company = newVal) } },
                        label = { Text("Company") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = entry.dates,
                        onValueChange = { newVal -> editList = editList.toMutableList().apply { this[i] = entry.copy(dates = newVal) } },
                        label = { Text("Dates") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Add new role
        CraftOutlineButton(
            text = "+ Add Role",
            onClick = { editList = editList + ExperienceEntry() },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))
        CraftAccentButton(text = "Save Experience", onClick = { onSave(editList) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun EducationEditor(
    education: List<EducationEntry>,
    onSave: (List<EducationEntry>) -> Unit
) {
    var editList by remember { mutableStateOf(education) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (editList.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CraftColors.WarningSoft,
                border = BorderStroke(1.dp, CraftColors.Warning.copy(alpha = 0.3f)),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎓", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No education entries yet", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CraftColors.InkPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Add your degrees and certifications to strengthen your resume.", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary)
                }
            }
        }
        
        editList.forEachIndexed { i, entry ->
            CraftCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Entry ${i + 1}", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CraftColors.InkPrimary, modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CraftColors.ErrorSoft,
                            modifier = Modifier.clickable { editList = editList.toMutableList().apply { removeAt(i) } },
                        ) {
                            Text("Remove", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CraftColors.Error)
                        }
                    }
                    OutlinedTextField(
                        value = entry.institution,
                        onValueChange = { newVal -> editList = editList.toMutableList().apply { this[i] = entry.copy(institution = newVal) } },
                        label = { Text("Institution") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = entry.degree,
                        onValueChange = { newVal -> editList = editList.toMutableList().apply { this[i] = entry.copy(degree = newVal) } },
                        label = { Text("Degree / Field of Study") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = entry.graduationDate,
                        onValueChange = { newVal -> editList = editList.toMutableList().apply { this[i] = entry.copy(graduationDate = newVal) } },
                        label = { Text("Graduation Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Add new entry
        CraftOutlineButton(
            text = "+ Add Education",
            onClick = { editList = editList + EducationEntry() },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))
        CraftAccentButton(text = "Save Education", onClick = { onSave(editList) }, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillsEditor(
    skills: List<String>,
    onSave: (List<String>) -> Unit
) {
    var editList by remember { mutableStateOf(skills) }
    var newSkill by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Current skills as removable chips
        if (editList.isNotEmpty()) {
            Text("Tap ✕ to remove a skill", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                editList.forEachIndexed { i, skill ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CraftColors.AccentSoft,
                        border = BorderStroke(1.dp, CraftColors.AccentBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(skill, fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkPrimary)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = CraftColors.InkTertiary.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { editList = editList.toMutableList().apply { removeAt(i) } },
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("✕", fontSize = 10.sp, color = CraftColors.InkSecondary)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text("No skills added yet. Add skills to strengthen your resume.", fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkSecondary)
        }

        // Add new skill field
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newSkill,
                onValueChange = { newSkill = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Add a skill") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CraftColors.Accent,
                    unfocusedBorderColor = CraftColors.Border
                ),
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CraftColors.Accent,
                modifier = Modifier.clickable {
                    val trimmed = newSkill.trim()
                    if (trimmed.isNotBlank()) {
                        // Support comma-separated batch add
                        val newSkills = trimmed.split(",").map { it.trim() }.filter { it.isNotBlank() && !editList.contains(it) }
                        editList = editList + newSkills
                        newSkill = ""
                    }
                },
            ) {
                Text("Add", modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontFamily = InterFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        Text("Tip: Paste multiple skills separated by commas to add them all at once.", fontFamily = InterFamily, fontSize = 11.sp, color = CraftColors.InkTertiary)

        Spacer(modifier = Modifier.height(8.dp))
        CraftAccentButton(text = "Save Skills (${editList.size})", onClick = {
            onSave(editList.filter { it.isNotBlank() })
        }, modifier = Modifier.fillMaxWidth())
    }
}
