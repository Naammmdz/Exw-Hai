package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Tab {
  HEARTH,
  MOMENTS,
  TIMELINE
}

data class CircleMember(
  val id: Int,
  val name: String,
  val relationship: String,
  val isSafe: Boolean,
  val timeText: String,
  val comment: String? = null,
  val avatarUrl: String
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
  var selectedTab by remember { mutableStateOf(Tab.HEARTH) }
  var checkedIn by remember { mutableStateOf(false) }
  var nudgeSentTo by remember { mutableStateOf<String?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }
  var pendingLucasRequest by remember { mutableStateOf(true) }

  // Circle members state list
  var circleMembers by remember {
    mutableStateOf(
      listOf(
        CircleMember(
          id = 1,
          name = "Sarah",
          relationship = "Best Friend",
          isSafe = true,
          timeText = "10m ago",
          comment = "Just got home, relaxing with some tea!",
          avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBbpVsjLgO8uGCHakEuiwV-6RFPI2mIH922q9L1T5BD36b5xJrpbsUNc7nyrfTV5ky2pXv0OKkhyUvjZpyT-molKGEUsCy5NpKtk9ZN1ZD3g1hE_XXzRgCy-sQokrnIsF4yivsvFSL0Vk8Wa3OZEeQEhkw4v46oKNrwu4-DVGhZ29L7M4P24fuVmMldi9SZHtqVSMLKjOJzMKxROcz5wwHGXl4zRjw-EQ5k4AAZt9sv3-ovWEsgeVziZ0WFGWzGT4RaE70vr8dJuCj0"
        ),
        CircleMember(
          id = 2,
          name = "Jason",
          relationship = "Brother",
          isSafe = false,
          timeText = "Due 5m ago",
          comment = null,
          avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuC_vcO18RUIMYAbp8Sat3hpn35LgOgEyXd7tP66nAYGejTcwMpEA8vASkbJ3YQmtkbuKW2mCyu3wg1kW1Nn0xZnWLEvl4LOkiG9q1ZmYdYVOrIP9Jwxkr9tWJMQejlwC26oqIG8sq0wy8accMvVD8gKWl76dKjN9dO8MTImL1DjMgzYa31rtVVx6N96I7c7_O9iM67-1iglhi9UG46IxmlpQHWlcVF_j5AWSiUrY6CAHDgMNuZgSM3oP9umG80nnTXi0KuB0mkrU7Ge"
        ),
        CircleMember(
          id = 3,
          name = "Mia",
          relationship = "Roommate",
          isSafe = true,
          timeText = "1h ago",
          comment = null,
          avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDDmwAg2VnwkmmTAOBwnUIiAJwAa1gVXcalss1UYBQa_Yf3Xu3J0lur4xjnwVv2z8g_2vBIKKyPeuYykYxeb1bwuMYafWP6HN1NI4yWBHs5y8VV0u6085SHZviIeU9RHg_3a3-WwoR_X_VXZjso-9-b6xGHvrw3Wu05-vUDtO-HY2haJnnvRgDZS4pPHo-XcFf6elAn5WpLn-VDfqaveoORpi2eNVs5vrqLM1_viis9t_ZN31wegpd4b8E9FrPrXC55DkoZl7K4dCN7"
        )
      )
    )
  }

  // Coroutine scope for transient notifications
  val coroutineScope = rememberCoroutineScope()

  Box(modifier = modifier.fillMaxSize().background(Cream)) {
    Column(modifier = Modifier.fillMaxSize()) {
      Spacer(modifier = Modifier.height(48.dp))

      // Tab selector rendering
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when (selectedTab) {
          Tab.HEARTH -> {
            // HEARTH Tab Content
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
            ) {
              // Header
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Good Morning, Alex",
                  color = Cocoa,
                  fontSize = 28.sp,
                  fontWeight = FontWeight.ExtraBold
                )
                Surface(
                  modifier = Modifier.size(40.dp),
                  shape = CircleShape,
                  color = Surface,
                  shadowElevation = 4.dp
                ) {
                  Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable { }) {
                    Icon(
                      imageVector = Icons.Rounded.Settings,
                      contentDescription = "Settings",
                      tint = Cocoa.copy(alpha = 0.6f),
                      modifier = Modifier.size(24.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(24.dp))

              // Safe Button Container
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(300.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                      drawCircle(
                        color = if (checkedIn) Apricot.copy(alpha = 0.3f) else Apricot.copy(alpha = 0.2f),
                        radius = if (checkedIn) size.width / 1.3f else size.width / 1.5f
                      )
                    }

                    Surface(
                      modifier = Modifier
                        .size(200.dp)
                        .clickable { checkedIn = !checkedIn },
                      shape = CircleShape,
                      color = Color.Transparent
                    ) {
                      Box(
                        modifier = Modifier
                          .fillMaxSize()
                          .background(
                            brush = Brush.linearGradient(
                              colors = listOf(Color(0xFFFFB5A7), Color(0xFFFFC4B8)),
                              start = Offset.Zero,
                              end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            ),
                            shape = CircleShape
                          )
                          .shadow(
                            elevation = if (checkedIn) 24.dp else 20.dp,
                            shape = CircleShape,
                            spotColor = Apricot,
                            ambientColor = Apricot
                          ),
                        contentAlignment = Alignment.Center
                      ) {
                        if (checkedIn) {
                          Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Confirmed Safe",
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                          )
                        } else {
                          Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                              "I'M SAFE",
                              color = Color.White,
                              fontSize = 24.sp,
                              fontWeight = FontWeight.ExtraBold,
                              letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                              modifier = Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .background(Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                          }
                        }
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(32.dp))

                  Surface(
                    color = Sage,
                    shape = CircleShape,
                    border = if (checkedIn) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else null,
                    shadowElevation = 8.dp
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(
                        horizontal = if (checkedIn) 32.dp else 24.dp,
                        vertical = if (checkedIn) 12.dp else 10.dp
                      )
                    ) {
                      Icon(
                        imageVector = if (checkedIn) Icons.Rounded.VerifiedUser else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Cocoa.copy(alpha = if (checkedIn) 0.8f else 0.6f),
                        modifier = Modifier.size(if (checkedIn) 20.dp else 18.dp)
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = if (checkedIn) "You are safe" else "Everything looks good",
                        color = Cocoa,
                        fontWeight = if (checkedIn) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = if (checkedIn) (-0.5).sp else 0.sp
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  Text(
                    text = if (checkedIn) "Last checked in: Just now" else "Last checked in: 10 mins ago",
                    color = Taupe,
                    fontSize = 14.sp,
                    fontWeight = if (checkedIn) FontWeight.SemiBold else FontWeight.Medium,
                    fontStyle = if (checkedIn) FontStyle.Italic else FontStyle.Normal
                  )
                }
              }

              Spacer(modifier = Modifier.height(24.dp))

              // Hug Card
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 32.dp, vertical = 16.dp),
                color = Surface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                shadowElevation = 4.dp
              ) {
                Row(
                  modifier = Modifier.padding(24.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(48.dp)
                      .clip(CircleShape)
                      .background(Sky.copy(alpha = 0.3f))
                      .border(2.dp, Color.White, CircleShape)
                  ) {
                    AsyncImage(
                      model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBbpVsjLgO8uGCHakEuiwV-6RFPI2mIH922q9L1T5BD36b5xJrpbsUNc7nyrfTV5ky2pXv0OKkhyUvjZpyT-molKGEUsCy5NpKtk9ZN1ZD3g1hE_XXzRgCy-sQokrnIsF4yivsvFSL0Vk8Wa3OZEeQEhkw4v46oKNrwu4-DVGhZ29L7M4P24fuVmMldi9SZHtqVSMLKjOJzMKxROcz5wwHGXl4zRjw-EQ5k4AAZt9sv3-ovWEsgeVziZ0WFGWzGT4RaE70vr8dJuCj0",
                      contentDescription = "Sarah",
                      modifier = Modifier.fillMaxSize(),
                      contentScale = ContentScale.Crop
                    )
                  }
                  Spacer(modifier = Modifier.width(16.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Sarah sent a hug", color = Cocoa, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("\"Thinking of you this morning!\"", color = Taupe, fontSize = 13.sp)
                  }
                  Text("🫂", fontSize = 20.sp)
                }
              }

              Spacer(modifier = Modifier.height(100.dp))
            }
          }

          Tab.MOMENTS -> {
            // MOMENTS Tab Content (My Circle)
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
            ) {
              // Header
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "My Circle",
                  color = Cocoa,
                  fontSize = 28.sp,
                  fontWeight = FontWeight.ExtraBold
                )
                Surface(
                  modifier = Modifier.size(40.dp),
                  shape = CircleShape,
                  color = Surface,
                  shadowElevation = 4.dp
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.clickable { showAddDialog = true }
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.GroupAdd,
                      contentDescription = "Add Member",
                      tint = Cocoa.copy(alpha = 0.6f),
                      modifier = Modifier.size(24.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Circle Members List
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                if (pendingLucasRequest) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(bottom = 8.dp)
                  ) {
                    Text(
                      text = "Pending Requests (1)",
                      color = Taupe,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.ExtraBold,
                      letterSpacing = 1.sp,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )

                    Surface(
                      color = Surface,
                      shape = RoundedCornerShape(32.dp),
                      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                      shadowElevation = 4.dp,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(12.dp),
                          modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                          Box(
                            modifier = Modifier
                              .size(56.dp)
                              .clip(CircleShape)
                              .background(Sky.copy(alpha = 0.1f))
                              .border(2.dp, Sky.copy(alpha = 0.3f), CircleShape)
                          ) {
                            AsyncImage(
                              model = "https://lh3.googleusercontent.com/aida-public/AB6AXuC1_Y2o9W1K_M9F7XpU6Zqj5lVz6mB1vHn_G7X8C9Y0Z1A2B3C4D5E6F7G8H9I0J1K2L3M4N5O6P7Q8R9S0T1U2V3W4X5Y6Z7",
                              contentDescription = "Lucas",
                              modifier = Modifier.fillMaxSize(),
                              contentScale = ContentScale.Crop
                            )
                          }

                          Column {
                            Text(
                              text = "Lucas",
                              color = Cocoa,
                              fontWeight = FontWeight.ExtraBold,
                              fontSize = 18.sp,
                              lineHeight = 22.sp
                            )
                            Text(
                              text = "Wants to join your circle",
                              color = Taupe,
                              fontWeight = FontWeight.Bold,
                              fontSize = 12.sp
                            )
                          }
                        }

                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                          Button(
                            onClick = {
                              val newMember = CircleMember(
                                id = circleMembers.size + 1,
                                name = "Lucas",
                                relationship = "Friend",
                                isSafe = true,
                                timeText = "Just now",
                                comment = "Glad to join your circle! 🌸",
                                avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuC1_Y2o9W1K_M9F7XpU6Zqj5lVz6mB1vHn_G7X8C9Y0Z1A2B3C4D5E6F7G8H9I0J1K2L3M4N5O6P7Q8R9S0T1U2V3W4X5Y6Z7"
                              )
                              circleMembers = circleMembers + newMember
                              pendingLucasRequest = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Apricot),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                          ) {
                            Text("Accept", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                          }

                          Button(
                            onClick = {
                              pendingLucasRequest = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Taupe.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                          ) {
                            Text("Decline", color = Taupe, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                          }
                        }
                      }
                    }
                  }
                }

                circleMembers.forEach { member ->
                  Surface(
                    color = Surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                          Box(
                            modifier = Modifier
                              .size(56.dp)
                              .clip(CircleShape)
                              .background(if (member.isSafe) Sky.copy(alpha = 0.1f) else Taupe.copy(alpha = 0.1f))
                              .border(
                                width = 2.dp,
                                color = if (member.isSafe) Sage.copy(alpha = 0.3f) else Apricot.copy(alpha = 0.3f),
                                shape = CircleShape
                              )
                          ) {
                            if (member.avatarUrl.isNotEmpty() && member.avatarUrl.startsWith("http")) {
                              AsyncImage(
                                model = member.avatarUrl,
                                contentDescription = member.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                              )
                            } else {
                              Box(
                                modifier = Modifier.fillMaxSize().background(Apricot),
                                contentAlignment = Alignment.Center
                              ) {
                                Text(
                                  text = member.name.take(1).uppercase(),
                                  color = Color.White,
                                  fontWeight = FontWeight.ExtraBold,
                                  fontSize = 20.sp
                                )
                              }
                            }
                          }

                          Column {
                            Text(
                              text = member.name,
                              color = Cocoa,
                              fontWeight = FontWeight.ExtraBold,
                              fontSize = 18.sp,
                              lineHeight = 22.sp
                            )
                            Text(
                              text = member.relationship,
                              color = Taupe,
                              fontWeight = FontWeight.Bold,
                              fontSize = 12.sp
                            )
                          }
                        }

                        Column(
                          horizontalAlignment = Alignment.End,
                          verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          val statusColor = if (member.isSafe) Sage else Apricot
                          val statusText = if (member.isSafe) "Safe" else "Awaiting Check-in"
                          val statusIcon = if (member.isSafe) Icons.Rounded.CheckCircle else Icons.Rounded.History
                          val textTint = if (member.isSafe) Cocoa else Color.White

                          Surface(
                            color = statusColor,
                            shape = RoundedCornerShape(12.dp)
                          ) {
                            Row(
                              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                              Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = textTint.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                              )
                              Text(
                                text = statusText,
                                color = textTint,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                              )
                            }
                          }

                          Text(
                            text = member.timeText,
                            color = Taupe,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                          )
                        }
                      }

                      // Member Status comment (if safe and has comment) or CTA button (if not safe)
                      if (member.isSafe && member.comment != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                          modifier = Modifier
                            .fillMaxWidth()
                            .background(Cream.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                        ) {
                          Text(
                            text = "\"${member.comment}\"",
                            color = Cocoa.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic
                          )
                        }
                      } else if (!member.isSafe) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                          onClick = {
                            nudgeSentTo = member.name
                            coroutineScope.launch {
                              delay(2500)
                              if (nudgeSentTo == member.name) {
                                nudgeSentTo = null
                              }
                            }
                          },
                          colors = ButtonDefaults.buttonColors(containerColor = Cream),
                          border = BorderStroke(2.dp, Apricot.copy(alpha = 0.2f)),
                          shape = RoundedCornerShape(12.dp),
                          modifier = Modifier.fillMaxWidth(),
                          contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Rounded.NotificationsActive,
                              contentDescription = null,
                              tint = Apricot,
                              modifier = Modifier.size(18.dp)
                            )
                            Text(
                              text = "Send a Gentle Nudge",
                              color = Apricot,
                              fontWeight = FontWeight.ExtraBold,
                              fontSize = 14.sp
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(100.dp))
            }
          }

          Tab.TIMELINE -> {
            // TIMELINE Tab Content
            Box(modifier = Modifier.fillMaxSize()) {
              // Timeline dashed line running down behind the cards
              Canvas(
                modifier = Modifier
                  .align(Alignment.TopStart)
                  .fillMaxHeight()
                  .width(2.dp)
                  .padding(start = 44.dp) // Aligns beautifully behind the center of left icon container (24dp screen padding + 20dp card padding)
              ) {
                val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                  floatArrayOf(12f, 12f), 0f
                )
                drawLine(
                  color = Cocoa,
                  alpha = 0.12f,
                  start = Offset(0f, 130.dp.toPx()),
                  end = Offset(0f, size.height - 110.dp.toPx()),
                  strokeWidth = 2.dp.toPx(),
                  pathEffect = pathEffect
                )
              }

              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState())
              ) {
                // Header
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 20.dp, bottom = 12.dp)
                ) {
                  Text(
                    text = "Timeline",
                    color = Cocoa,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Your cozy history",
                    color = Taupe,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Timeline Feed Items
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 120.dp),
                  verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                  // Card 1: Check-in: Everything is okay (9:00 AM)
                  Surface(
                    color = Surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 24.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(20.dp),
                      verticalAlignment = Alignment.Top,
                      horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(40.dp)
                          .background(Sage.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.CheckCircle,
                          contentDescription = null,
                          tint = Sage,
                          modifier = Modifier.size(24.dp)
                        )
                      }

                      Column(modifier = Modifier.weight(1f)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(
                            text = "Check-in: Everything is okay",
                            color = Cocoa,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                            lineHeight = 20.sp
                          )
                          Spacer(modifier = Modifier.width(8.dp))
                          Text(
                            text = "9:00 AM",
                            color = Taupe,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                          )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                          text = "Automatic safety heartbeat sent.",
                          color = Taupe,
                          fontSize = 14.sp
                        )
                      }
                    }
                  }

                  // Card 2: Sarah viewed your status (10:15 AM)
                  Surface(
                    color = Surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 24.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(20.dp),
                      verticalAlignment = Alignment.Top,
                      horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(40.dp)
                          .clip(CircleShape)
                          .border(2.dp, Cream, CircleShape)
                      ) {
                        AsyncImage(
                          model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBbpVsjLgO8uGCHakEuiwV-6RFPI2mIH922q9L1T5BD36b5xJrpbsUNc7nyrfTV5ky2pXv0OKkhyUvjZpyT-molKGEUsCy5NpKtk9ZN1ZD3g1hE_XXzRgCy-sQokrnIsF4yivsvFSL0Vk8Wa3OZEeQEhkw4v46oKNrwu4-DVGhZ29L7M4P24fuVmMldi9SZHtqVSMLKjOJzMKxROcz5wwHGXl4zRjw-EQ5k4AAZt9sv3-ovWEsgeVziZ0WFGWzGT4RaE70vr8dJuCj0",
                          contentDescription = "Sarah",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop
                        )
                      }

                      Column(modifier = Modifier.weight(1f)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(
                            text = "Sarah viewed your status",
                            color = Cocoa,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                            lineHeight = 20.sp
                          )
                          Spacer(modifier = Modifier.width(8.dp))
                          Text(
                            text = "10:15 AM",
                            color = Taupe,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                          )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                          text = "A loved one checked in on you.",
                          color = Taupe,
                          fontSize = 14.sp
                        )
                      }
                    }
                  }

                  // Card 3: Moment Shared (11:30 AM)
                  Surface(
                    color = Surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 24.dp)
                  ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                      ) {
                        Box(
                          modifier = Modifier
                            .size(40.dp)
                            .background(Apricot.copy(alpha = 0.2f), CircleShape),
                          contentAlignment = Alignment.Center
                        ) {
                          Icon(
                            imageVector = Icons.Rounded.LocalFlorist,
                            contentDescription = null,
                            tint = Apricot,
                            modifier = Modifier.size(24.dp)
                          )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Text(
                              text = "Moment Shared",
                              color = Cocoa,
                              fontWeight = FontWeight.ExtraBold,
                              fontSize = 16.sp,
                              modifier = Modifier.weight(1f),
                              lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                              text = "11:30 AM",
                              color = Taupe,
                              fontSize = 12.sp,
                              fontWeight = FontWeight.Bold
                            )
                          }
                          Spacer(modifier = Modifier.height(4.dp))
                          Text(
                            text = "\"Cozy coffee break ☕️\"",
                            color = Taupe,
                            fontSize = 14.sp
                          )
                        }
                      }

                      Spacer(modifier = Modifier.height(16.dp))

                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(160.dp)
                          .clip(RoundedCornerShape(16.dp))
                          .background(Cream)
                      ) {
                        AsyncImage(
                          model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDZLxH_7TUwOJf6CI792F-92EBaqhvN3pfXUDEi1FcrYmDlB5dDOi5gVRH7WW6KyegX3yo4dM5tWD57kV7SS8th6tD1wEi-zmhxQlLB1HGdGeWipOG78yvEChvZfayMki57tb71zeTZUOjrAeniN3-8UZ3deotl0f66weZ1-SMPvPKK5ZUq19wjzHdK6pPMBvm-sdvRgivEGsdPg_vZWsIiZRVbd0_Zw7TeN0FOyHR2Mtq8WlT2QIhMcoA9HKn2sHCxR_NvICcmSx0Y",
                          contentDescription = "Coffee Cup",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop
                        )
                      }
                    }
                  }

                  // Card 4: Nightly Check-in (11:00 PM) - Opacity 60%
                  Surface(
                    color = Surface.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 24.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(20.dp),
                      verticalAlignment = Alignment.Top,
                      horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(40.dp)
                          .background(Sky.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.NightsStay,
                          contentDescription = null,
                          tint = Sky,
                          modifier = Modifier.size(24.dp)
                        )
                      }

                      Column(modifier = Modifier.weight(1f)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(
                            text = "Nightly Check-in",
                            color = Cocoa.copy(alpha = 0.7f),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                            lineHeight = 20.sp
                          )
                          Spacer(modifier = Modifier.width(8.dp))
                          Text(
                            text = "11:00 PM",
                            color = Taupe.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                          )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                          text = "Sleep mode activated.",
                          color = Taupe.copy(alpha = 0.7f),
                          fontSize = 14.sp
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Overlay transient safe notified sliding notification
    if (checkedIn && selectedTab == Tab.HEARTH) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 96.dp)
          .padding(horizontal = 32.dp)
          .fillMaxWidth()
      ) {
        Surface(
          color = Cocoa.copy(alpha = 0.9f),
          shape = RoundedCornerShape(16.dp),
          shadowElevation = 12.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.Group,
              contentDescription = null,
              tint = Apricot,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Your circle has been notified.",
              color = Cream,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Overlay transient nudge notification
    if (nudgeSentTo != null) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 96.dp)
          .padding(horizontal = 32.dp)
          .fillMaxWidth()
      ) {
        Surface(
          color = Cocoa.copy(alpha = 0.95f),
          shape = RoundedCornerShape(16.dp),
          shadowElevation = 12.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.NotificationsActive,
              contentDescription = null,
              tint = Apricot,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Sent a gentle nudge to $nudgeSentTo 🌸",
              color = Cream,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Bottom Navigation
    Surface(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth(),
      color = Color.White.copy(alpha = 0.95f),
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      shadowElevation = 16.dp
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 40.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        BottomNavItem(
          icon = Icons.Rounded.Home,
          label = "HEARTH",
          isSelected = selectedTab == Tab.HEARTH,
          onClick = { selectedTab = Tab.HEARTH }
        )
        BottomNavItem(
          icon = Icons.Rounded.LocalFlorist,
          label = "MOMENTS",
          isSelected = selectedTab == Tab.MOMENTS,
          onClick = { selectedTab = Tab.MOMENTS }
        )
        BottomNavItem(
          icon = Icons.Rounded.Schedule,
          label = "TIMELINE",
          isSelected = selectedTab == Tab.TIMELINE,
          onClick = { selectedTab = Tab.TIMELINE }
        )
      }
    }

    // Cozy Custom Add Member Dialog
    if (showAddDialog) {
      var name by remember { mutableStateOf("") }
      var relationship by remember { mutableStateOf("") }

      AlertDialog(
        onDismissRequest = { showAddDialog = false },
        confirmButton = {
          Button(
            onClick = {
              if (name.isNotEmpty() && relationship.isNotEmpty()) {
                val newMember = CircleMember(
                  id = circleMembers.size + 1,
                  name = name,
                  relationship = relationship,
                  isSafe = true,
                  timeText = "Just added",
                  comment = "Glad to be part of your hearth!",
                  avatarUrl = ""
                )
                circleMembers = circleMembers + newMember
                showAddDialog = false
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Apricot),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showAddDialog = false }) {
            Text("Cancel", color = Taupe)
          }
        },
        title = {
          Text(
            text = "Add to your Circle",
            color = Cocoa,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
          )
        },
        text = {
          Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Invite a direct loved one or friend to check-in on Cozy Companion.",
              color = Taupe,
              fontSize = 14.sp
            )

            OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = { Text("Name") },
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Apricot,
                focusedLabelColor = Apricot,
                unfocusedBorderColor = Cocoa.copy(alpha = 0.2f)
              ),
              modifier = Modifier.fillMaxWidth(),
              singleLine = true
            )

            OutlinedTextField(
              value = relationship,
              onValueChange = { relationship = it },
              label = { Text("Relationship (e.g. Spouse)") },
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Apricot,
                focusedLabelColor = Apricot,
                unfocusedBorderColor = Cocoa.copy(alpha = 0.2f)
              ),
              modifier = Modifier.fillMaxWidth(),
              singleLine = true
            )
          }
        },
        containerColor = Cream,
        shape = RoundedCornerShape(24.dp)
      )
    }
  }
}

@Composable
fun BottomNavItem(
  icon: ImageVector,
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val color = if (isSelected) Apricot else Taupe
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = color,
      modifier = Modifier.size(28.dp)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      color = color,
      fontSize = 10.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 1.sp
    )
  }
}
