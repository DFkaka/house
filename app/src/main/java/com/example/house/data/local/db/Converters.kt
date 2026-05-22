
package com.example.house.data.local.db

import androidx.room.TypeConverter
import com.example.house.data.local.entity.RateType
import com.example.house.data.local.entity.RoomStatus
import com.example.house.data.local.entity.SettleStatus

class Converters {
    @TypeConverter fun roomStatusToString(s: RoomStatus): String = s.name
    @TypeConverter fun stringToRoomStatus(s: String): RoomStatus = RoomStatus.valueOf(s)

    @TypeConverter fun settleStatusToString(s: SettleStatus): String = s.name
    @TypeConverter fun stringToSettleStatus(s: String): SettleStatus = SettleStatus.valueOf(s)

    @TypeConverter fun rateTypeToString(r: RateType): String = r.name
    @TypeConverter fun stringToRateType(s: String): RateType = RateType.valueOf(s)
}
