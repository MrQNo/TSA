import com.github.nscala_time.time.Imports.DateTime
import org.joda.time.format.ISODateTimeFormat
import upickle.default._

object DateTimeRW:
  val fmt = ISODateTimeFormat.dateTime()
  given ReadWriter[DateTime] = readwriter[ujson.Value].bimap[DateTime](
    dt => fmt.print(dt),
    dtstr => fmt.parseDateTime(dtstr.str)
  )

val dt = DateTime.now()
val jsonDateString = write(dt)
val dt2: DateTime = read(dt.toString)
