package com.icecream.diy.data.local

import android.content.Context
import com.icecream.diy.data.model.CustomerConfig
import com.icecream.diy.data.model.Flavor
import com.icecream.diy.data.model.LevelConfig
import com.icecream.diy.data.model.TargetLayer
import org.json.JSONArray

object LevelDataProvider {
    private var cachedLevels: List<LevelConfig>? = null

    fun loadLevels(context: Context): List<LevelConfig> {
        cachedLevels?.let { return it }

        return try {
            val jsonString = context.assets.open("levels.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<LevelConfig>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val lvlNum = obj.getInt("levelNumber")
                val hasMini = obj.optBoolean("hasMinigame", false)

                // Available flavors
                val flavArray = obj.getJSONArray("availableFlavors")
                val flavors = mutableListOf<Flavor>()
                for (f in 0 until flavArray.length()) {
                    val fName = flavArray.getString(f)
                    Flavor.values().find { it.displayName.equals(fName, ignoreCase = true) }?.let {
                        flavors.add(it)
                    }
                }

                // Target layers
                val layersArray = obj.getJSONArray("targetLayers")
                val targetLayers = mutableListOf<TargetLayer>()
                for (l in 0 until layersArray.length()) {
                    val layerObj = layersArray.getJSONObject(l)
                    val layerFlavArray = layerObj.getJSONArray("flavors")
                    val layerFlavors = mutableListOf<Flavor>()
                    for (lf in 0 until layerFlavArray.length()) {
                        val lfName = layerFlavArray.getString(lf)
                        Flavor.values().find { it.displayName.equals(lfName, ignoreCase = true) }?.let {
                            layerFlavors.add(it)
                        }
                    }
                    val prop = layerObj.getDouble("proportion").toFloat()
                    targetLayers.add(TargetLayer(layerFlavors, prop))
                }

                val sprinkles = obj.optInt("sprinklesCount", 0)
                val cherry = obj.optBoolean("hasCherry", false)
                val cones = obj.optInt("extraCones", 1)

                val custObj = obj.optJSONObject("customer")
                val customer = if (custObj != null) {
                    CustomerConfig(
                        id = custObj.optString("id", "cust_1"),
                        name = custObj.optString("name", "Guest"),
                        gender = custObj.optString("gender", "female")
                    )
                } else {
                    CustomerConfig()
                }

                list.add(
                    LevelConfig(
                        levelNumber = lvlNum,
                        hasMinigame = hasMini,
                        availableFlavors = flavors,
                        targetLayers = targetLayers,
                        sprinklesCount = sprinkles,
                        hasCherry = cherry,
                        extraCones = cones,
                        customer = customer
                    )
                )
            }
            cachedLevels = list
            list
        } catch (_: Exception) {
            fallbackLevels
        }
    }

    fun getLevel(context: Context?, number: Int): LevelConfig {
        val list = if (context != null) loadLevels(context) else (cachedLevels ?: fallbackLevels)
        return list.find { it.levelNumber == number } ?: run {
            val all = Flavor.values()
            val f1 = all[(number - 1) % all.size]
            val f2 = all[number % all.size]
            LevelConfig(
                levelNumber = number,
                hasMinigame = number % 2 == 0,
                availableFlavors = listOf(Flavor.VANILLA, Flavor.STRAWBERRY, Flavor.CHOCOLATE),
                targetLayers = listOf(TargetLayer(listOf(f1), 0.5f), TargetLayer(listOf(f2), 0.5f)),
                sprinklesCount = 15,
                hasCherry = true,
                extraCones = 1
            )
        }
    }

    private val fallbackLevels: List<LevelConfig> = listOf(
        LevelConfig(
            levelNumber = 1,
            hasMinigame = false,
            availableFlavors = listOf(Flavor.VANILLA, Flavor.STRAWBERRY, Flavor.CHOCOLATE),
            targetLayers = listOf(TargetLayer(listOf(Flavor.VANILLA), 1.0f)),
            sprinklesCount = 0,
            hasCherry = false,
            extraCones = 1
        ),
        LevelConfig(
            levelNumber = 2,
            hasMinigame = true,
            availableFlavors = listOf(Flavor.VANILLA, Flavor.STRAWBERRY, Flavor.CHOCOLATE),
            targetLayers = listOf(
                TargetLayer(listOf(Flavor.VANILLA), 0.5f),
                TargetLayer(listOf(Flavor.STRAWBERRY), 0.5f)
            ),
            sprinklesCount = 0,
            hasCherry = false,
            extraCones = 1
        )
    )
}
