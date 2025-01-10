package ru.application.homemedkit.helpers

import android.content.Context
import android.icu.math.BigDecimal
import android.icu.text.DecimalFormat
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import ru.application.homemedkit.HomeMeds.Companion.database
import ru.application.homemedkit.R
import ru.application.homemedkit.R.string.text_error
import ru.application.homemedkit.R.string.text_success
import ru.application.homemedkit.data.dto.Intake
import ru.application.homemedkit.data.dto.Medicine
import ru.application.homemedkit.data.dto.Technical
import ru.application.homemedkit.models.events.Response
import ru.application.homemedkit.models.states.IntakeState
import ru.application.homemedkit.models.states.MedicineState
import ru.application.homemedkit.models.states.TechnicalState
import ru.application.homemedkit.network.models.bio.BioData
import ru.application.homemedkit.network.models.medicine.DrugsData
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.reflect.KClass

val LOCALE @Composable get() = LocalConfiguration.current.locales[0]
val ZONE = ZoneId.systemDefault().rules.getOffset(Instant.now())
val FORMAT_S = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val FORMAT_DH = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm")
val FORMAT_DMMMMY @Composable get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(LOCALE)
val FORMAT_DME @Composable get() = DateTimeFormatter.ofPattern("d MMMM, E", LOCALE)
val FORMAT_H = DateTimeFormatter.ofPattern("H:mm")
private val FORMAT_MY = DateTimeFormatter.ofPattern("MM/yyyy")

@Composable
fun toExpDate(milli: Long) = if (milli > 0) getDateTime(milli).format(FORMAT_DMMMMY) else BLANK

fun toTimestamp(month: Int, year: Int) = LocalDateTime.of(
    year, month, YearMonth.of(year, month).lengthOfMonth(),
    LocalTime.MAX.hour, LocalTime.MAX.minute
).toInstant(ZONE).toEpochMilli()

fun inCard(milli: Long) = if (milli == -1L) BLANK else getDateTime(milli).format(FORMAT_MY)

fun lastAlarm(date: String, time: LocalTime) = LocalDateTime.of(
    LocalDate.parse(date, FORMAT_S), time
).toInstant(ZONE).toEpochMilli()

fun expirationCheckTime(): Long {
    var unix = LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 0))

    if (unix.toInstant(ZONE).toEpochMilli() < System.currentTimeMillis()) unix = unix.plusDays(1)

    return unix.toInstant(ZONE).toEpochMilli()
}

fun getDateTime(milli: Long) = Instant.ofEpochMilli(milli).atZone(ZONE)

fun longSeconds(start: String, time: List<LocalTime>) = ArrayList<Long>(time.size).apply {
    time.sortedWith(compareBy { it }).forEach { localTime ->
        val localDate = LocalDate.parse(start, FORMAT_S)
        var unix = LocalDateTime.of(localDate, localTime)

        while (unix.toInstant(ZONE).toEpochMilli() < System.currentTimeMillis()) {
            unix = unix.plusDays(1)
        }

        add(unix.toInstant(ZONE).toEpochMilli())
    }
}

fun showToast(success: Boolean, context: Context) = Toast.makeText(
    context, context.getString(if (success) text_success else text_error),
    Toast.LENGTH_LONG).show()

fun formName(name: String) = name.substringBefore(" ")

fun decimalFormat(text: Any?): String {
    val amount = try {
        text.toString().toDouble()
    } catch (e: NumberFormatException) {
        0.0
    }

    return DecimalFormat.getInstance().apply {
        maximumFractionDigits = 4
        roundingMode = BigDecimal.ROUND_HALF_EVEN
    }.format(amount)
}

fun createNotificationChannel(
    context: Context,
    channelId: String,
    channelName: Int,
    channelDescription: Int = R.string.channel_name
) = NotificationManagerCompat.from(context).apply {
    deleteNotificationChannel(CHANNEL_ID_LEGACY)
    createNotificationChannel(
        NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_MAX)
            .setName(context.getString(channelName))
            .setDescription(context.getString(channelDescription))
            .setSound(
                RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
            .build()
    )
}

fun <T: Any> NavBackStackEntry?.isCurrentRoute(route: KClass<T>) =
    this?.destination?.hierarchy?.any { it.hasRoute(route) } == true

fun NavHostController.toBottomBarItem(route: Any) = navigate(route) {
    launchSingleTop = true
    restoreState = true

    popUpTo(this@toBottomBarItem.graph.findStartDestination().id) {
        saveState = true
    }
}

fun Medicine.toState() = MedicineState(
    adding = false,
    editing = false,
    default = true,
    fetch = Response.Default,
    id = id,
    kits = database.kitDAO().getIdList(id).toMutableStateList(),
    cis = cis,
    productName = productName,
    nameAlias = nameAlias,
    expDate = expDate,
    prodFormNormName = prodFormNormName,
    structure = structure,
    prodDNormName = prodDNormName,
    prodAmount = prodAmount.toString(),
    doseType = doseType,
    phKinetics = phKinetics,
    recommendations = recommendations,
    storageConditions = storageConditions,
    comment = comment,
    image = image,
    technical = TechnicalState(
        scanned = technical.scanned,
        verified = technical.verified
    )
)

fun MedicineState.toMedicine() = Medicine(
    id = id,
    cis = cis,
    productName = productName,
    nameAlias = nameAlias,
    expDate = expDate,
    prodFormNormName = prodFormNormName,
    structure = structure,
    prodDNormName = prodDNormName,
    prodAmount = prodAmount.ifEmpty { "0.0" }.toDouble(),
    doseType = doseType,
    phKinetics = phKinetics,
    recommendations = recommendations,
    storageConditions = storageConditions,
    comment = comment,
    image = image,
    technical = Technical(
        scanned = cis.isNotBlank(),
        verified = technical.verified
    )
)

fun IntakeState.toIntake(time: List<LocalTime>) = Intake(
    intakeId = intakeId,
    medicineId = medicineId,
    amount = amount.toDouble(),
    interval = interval.toInt(),
    foodType = foodType,
    time = time,
    period = period.toInt(),
    startDate = startDate,
    finalDate = finalDate,
    fullScreen = fullScreen,
    noSound = noSound,
    preAlarm = preAlarm,
    cancellable = cancellable
)

fun DrugsData.toMedicine() = Medicine(
    productName = prodDescLabel,
    expDate = expireDate,
    prodFormNormName = foiv.prodFormNormName,
    prodDNormName = foiv.prodDNormName.orEmpty(),
    doseType = Types.getDoseType(foiv.prodFormNormName),
    phKinetics = vidalData?.phKinetics.orEmpty(),
    technical = Technical(scanned = true, verified = true),
    prodAmount = foiv.prodPack1Size?.let {
        it.toDouble() * (foiv.prodPack12?.toDoubleOrNull() ?: 1.0)
    } ?: 0.0
)

fun BioData.toBio() = Medicine(
    productName = productName,
    expDate = expireDate,
    prodDNormName = productProperty.unitVolumeWeight.orEmpty(),
    prodAmount = productProperty.quantityInPack ?: 0.0,
    phKinetics = productProperty.applicationArea.orEmpty(),
    recommendations = productProperty.recommendForUse.orEmpty(),
    storageConditions = productProperty.storageConditions.orEmpty(),
    structure = productProperty.structure.orEmpty(),
    technical = Technical(scanned = true, verified = true),
    prodFormNormName = productProperty.releaseForm.orEmpty().substringBefore(" ").uppercase(),
    doseType = Types.getDoseType(productProperty.releaseForm.orEmpty()),
)