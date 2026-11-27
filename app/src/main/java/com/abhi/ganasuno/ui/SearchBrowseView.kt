package com.abhi.ganasuno.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.abhi.ganasuno.R
import com.abhi.ganasuno.data.Song

/** Search landing page. Cards issue real queries; no fabricated listening history. */
class SearchBrowseView(private val context: Context, private val target: LinearLayout) {
    private fun dp(n: Int) = (n * context.resources.displayMetrics.density + .5f).toInt()
    private fun round(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun label(value: String, size: Float, bold: Boolean = false) = TextView(context).apply {
        text = value; textSize = size
        if (bold) setTypeface(null, Typeface.BOLD)
        // Keep the user's light/dark theme text colours.
        val attr = android.util.TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.textColorPrimary, attr, true)) {
            if (attr.resourceId != 0) runCatching { androidx.core.content.ContextCompat.getColorStateList(context, attr.resourceId)?.let { setTextColor(it) } }
            else if (attr.type in android.util.TypedValue.TYPE_FIRST_COLOR_INT..android.util.TypedValue.TYPE_LAST_COLOR_INT) setTextColor(attr.data)
        }
    }
    private fun column() = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP }
    private fun weighted(end: Int = 0) = LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(end) }
    private fun action(view: View, description: String, onClick: () -> Unit) {
        view.isClickable = true; view.isFocusable = true; view.contentDescription = description
        view.setOnClickListener { onClick() }
    }
    private data class Genre(val name: String, val query: String, val color: Int, val art: Int)
    private val genres = listOf(
        Genre("New Releases", "new music releases", 0xff6414ff.toInt(), R.drawable.genre_new_releases),
        Genre("Pop", "pop songs", 0xffa923b0.toInt(), R.drawable.genre_pop),
        Genre("Hip-Hop", "hip hop songs", 0xff2c2c2e.toInt(), R.drawable.genre_hip_hop),
        Genre("Holidays", "holiday music", 0xff006b59.toInt(), R.drawable.genre_holidays),
        Genre("Bollywood", "bollywood songs", 0xff007d57.toInt(), R.drawable.genre_bollywood),
        Genre("Romantic", "romantic songs", 0xffb00000.toInt(), R.drawable.genre_romantic),
        Genre("Punjabi", "punjabi songs", 0xff9c4b15.toInt(), R.drawable.genre_punjabi),
        Genre("Lofi", "lofi music", 0xff3549a8.toInt(), R.drawable.genre_lofi),
        Genre("Workout", "workout music", 0xff795315.toInt(), R.drawable.genre_workout),
        Genre("Sleep", "sleep music", 0xff51408c.toInt(), R.drawable.genre_sleep),
        Genre("Indie", "indie music", 0xff9a305d.toInt(), R.drawable.genre_indie),
        Genre("Rock", "rock songs", 0xff42525e.toInt(), R.drawable.genre_rock),
        Genre("Trending", "trending music", 0xffa14a25.toInt(), R.drawable.genre_trending),
        Genre("Electronic", "electronic music", 0xff22478b.toInt(), R.drawable.genre_electronic),
        Genre("Classical", "classical music", 0xff76502e.toInt(), R.drawable.genre_classical),
        Genre("Party", "party music", 0xff95245b.toInt(), R.drawable.genre_party),
        Genre("Podcasts", "music podcasts", 0xff295965.toInt(), R.drawable.genre_podcasts),
        Genre("Regional Music", "regional Indian songs", 0xff7343a4.toInt(), R.drawable.genre_regional_music),
        Genre("Top Charts", "top music charts", 0xff2456a0.toInt(), R.drawable.genre_top_charts),
        Genre("Chill", "chill songs", 0xff00784f.toInt(), R.drawable.genre_chill)
    )
    fun categoryName(query: String): String? = genres.firstOrNull { it.query.equals(query.trim(), true) || it.name.equals(query.trim(), true) }?.name
    fun render(history: List<String>, saved: List<Song>, search: (String) -> Unit, showHistory: () -> Unit) {
        val panel = column().apply { setPadding(dp(20), dp(4), dp(20), dp(16)) }
        target.addView(panel, LinearLayout.LayoutParams(-1, -2))
        val heading = row().apply { gravity = Gravity.CENTER_VERTICAL }
        heading.addView(label("Frequently Searched", 21f, true), weighted())
        val historyButton = label("History", 13f, true).apply {
            setPadding(dp(12), dp(10), dp(12), dp(10)); minHeight = dp(48); gravity = Gravity.CENTER; background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.glass_profile_field); setTextColor(if ((context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) 0xffc4c4c4.toInt() else 0xff595959.toInt())
        }
        action(historyButton, "Open search history", showHistory)
        heading.addView(historyButton)
        panel.addView(heading)
        val queries = history.take(15)
        if (queries.isEmpty()) {
            panel.addView(label("Your frequent searches will appear here. Try a category below.", 14f).apply {
                setPadding(0, dp(6), 0, dp(14)); alpha = .8f
            })
        } else {
            val cards = row()
            queries.forEach { query ->
                val song = saved.firstOrNull { it.title.equals(query, true) || it.artist.equals(query, true) }
                val isArtist = song?.artist?.equals(query, true) == true
                val card = column().apply { gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, dp(4), 0, dp(8)) }
                val art = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = round(0xff303036.toInt(), if (isArtist) 60 else 12)
                    clipToOutline = true
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                val artSize = ((context.resources.displayMetrics.widthPixels / context.resources.displayMetrics.density - 64f) / 3f).toInt().coerceIn(68, 106)
                card.addView(art, LinearLayout.LayoutParams(dp(artSize), dp(artSize)))
                ImageLoader.load(song?.thumb.orEmpty(), art, R.drawable.ic_placeholder)
                card.addView(label(query, 13f, true).apply {
                    gravity = Gravity.CENTER; maxLines = 2; ellipsize = TextUtils.TruncateAt.END; setPadding(0, dp(6), 0, 0)
                }, LinearLayout.LayoutParams(-1, -2))
                if (song != null && !isArtist && song.artist.isNotBlank()) card.addView(label(song.artist, 12f).apply {
                    gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END; alpha = .8f
                }, LinearLayout.LayoutParams(-1, -2))
                action(card, "Search again: $query") { search(query) }
                cards.addView(card, LinearLayout.LayoutParams(dp(artSize+12),-2).apply{marginEnd=dp(8)})
            }
            panel.addView(android.widget.HorizontalScrollView(context).apply{isHorizontalScrollBarEnabled=false;addView(cards)}, LinearLayout.LayoutParams(-1,-2))
        }
        panel.addView(label("Categories", 22f, true).apply { setPadding(0, dp(10), 0, dp(12)) })
        genres.chunked(2).forEach { pair ->
            val line = row()
            pair.forEachIndexed { index, genre ->
                val tile = GenreGlassCard(context, genre.name, genre.color, genre.art)
                action(tile, "Browse ${genre.name}") { search(genre.query) }
                line.addView(tile, weighted(if (index == 0) 12 else 0).apply { height = dp(112 + ((context.resources.configuration.fontScale - 1f).coerceAtLeast(0f) * 32).toInt()) })
            }
            panel.addView(line, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        }
    }
}
