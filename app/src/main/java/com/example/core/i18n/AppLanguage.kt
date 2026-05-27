package com.example.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.CircleStatus

enum class AppLanguage(val tag: String) {
  English("en"),
  Vietnamese("vi");

  companion object {
    fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: English
  }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.English }

fun AppLanguage.next(): AppLanguage = when (this) {
  AppLanguage.English -> AppLanguage.Vietnamese
  AppLanguage.Vietnamese -> AppLanguage.English
}

@Composable
fun appString(id: Int): String {
  if (LocalAppLanguage.current == AppLanguage.English) return stringResource(id)
  return when (id) {
    R.string.sign_in -> "Đăng nhập"
    R.string.sign_up -> "Tạo tài khoản"
    R.string.email -> "Email"
    R.string.password -> "Mật khẩu"
    R.string.full_name -> "Họ tên"
    R.string.forgot_password -> "Quên mật khẩu?"
    R.string.welcome_title -> "Một bàn tay dịu dàng luôn ở bên"
    R.string.welcome_subtitle -> "Xác nhận an toàn cho người sống độc lập."
    R.string.onboarding_one -> "Một bàn tay dịu dàng luôn ở bên"
    R.string.onboarding_two -> "Xác nhận an toàn thật đơn giản"
    R.string.onboarding_three -> "Cảm nhận sự ấm áp"
    R.string.get_started -> "Bắt đầu"
    R.string.im_safe -> "Tôi an toàn"
    R.string.circle_notified -> "Vòng thân của bạn đã được thông báo."
    R.string.hearth -> "Mái ấm"
    R.string.circle -> "Vòng thân"
    R.string.timeline -> "Dòng thời gian"
    R.string.moments -> "Khoảnh khắc"
    R.string.safety -> "An toàn"
    R.string.crisis -> "Khẩn cấp"
    R.string.plans -> "Gói dịch vụ"
    R.string.add_friend -> "Thêm bạn"
    R.string.share_moment -> "Chia sẻ khoảnh khắc"
    R.string.emergency_contacts -> "Liên hệ khẩn cấp"
    R.string.safety_rhythm -> "Nhịp an toàn"
    R.string.logout -> "Đăng xuất"
    else -> stringResource(id)
  }
}

@Composable
fun t(en: String, vi: String): String = if (LocalAppLanguage.current == AppLanguage.English) en else vi

fun tr(language: AppLanguage, en: String, vi: String): String = if (language == AppLanguage.English) en else vi

@Composable
fun localizedEventText(value: String): String = when (value) {
  "Check-in confirmed" -> t("Check-in confirmed", "Đã xác nhận an toàn")
  "Your circle has been notified." -> t("Your circle has been notified.", "Vòng thân của bạn đã được thông báo.")
  "Circle invitation sent" -> t("Circle invitation sent", "Đã gửi lời mời vào vòng thân")
  "Circle invitation accepted" -> t("Circle invitation accepted", "Đã chấp nhận lời mời")
  "Circle invitation declined" -> t("Circle invitation declined", "Đã từ chối lời mời")
  "Request status updated." -> t("Request status updated.", "Trạng thái lời mời đã được cập nhật.")
  "Gentle nudge sent" -> t("Gentle nudge sent", "Đã gửi nhắc nhở nhẹ nhàng")
  "Moment shared" -> t("Moment shared", "Đã chia sẻ khoảnh khắc")
  "Emergency contact saved" -> t("Emergency contact saved", "Đã lưu liên hệ khẩn cấp")
  "Safety rhythm updated" -> t("Safety rhythm updated", "Đã cập nhật nhịp an toàn")
  "Morning check-in" -> t("Morning check-in", "Xác nhận an toàn buổi sáng")
  "Automatic safety heartbeat sent." -> t("Automatic safety heartbeat sent.", "Tín hiệu an toàn tự động đã được gửi.")
  "Morning coffee ritual" -> t("Morning coffee ritual", "Thói quen cà phê sáng")
  else -> value
}

@Composable
fun localizedRelationship(value: String): String = when (value) {
  "Best friend" -> t("Best friend", "Bạn thân")
  "Family" -> t("Family", "Gia đình")
  "Trusted contact" -> t("Trusted contact", "Liên hệ tin cậy")
  else -> value
}

@Composable
fun localizedRhythmLabel(value: String): String = when (value) {
  "Wakeup Check" -> t("Wakeup Check", "Xác nhận khi thức dậy")
  "Bedtime Check" -> t("Bedtime Check", "Xác nhận trước khi ngủ")
  else -> value
}

@Composable
fun localizedCircleStatus(status: CircleStatus): String = when (status) {
  CircleStatus.Pending -> t("pending", "đang chờ")
  CircleStatus.Accepted -> t("accepted", "đã chấp nhận")
  CircleStatus.Declined -> t("declined", "đã từ chối")
}

@Composable
fun friendlyTimeText(value: String?): String {
  if (value.isNullOrBlank()) return t("not yet", "chưa có")
  return value.substringAfter('T', value).take(5).ifBlank { value }
}
