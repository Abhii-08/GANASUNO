package com.abhi.ganasuno.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.abhi.ganasuno.R
import com.abhi.ganasuno.data.Song
import com.abhi.ganasuno.databinding.ItemSongBinding

class SongAdapter(
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit = {},
) : RecyclerView.Adapter<SongAdapter.VH>() {
    var gridMode:Boolean=false
        set(value){if(field!=value){field=value;notifyDataSetChanged()}}
    override fun getItemViewType(position:Int)=if(gridMode)1 else 0
    private var items: List<Song> = emptyList()

    var playingId: String? = null
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    class VH(
        val b: ItemSongBinding,
    ) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<Song>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): VH {
        val binding=ItemSongBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        if(viewType==1){
            val root=binding.root as android.widget.LinearLayout
            root.orientation=android.widget.LinearLayout.VERTICAL
            root.gravity=android.view.Gravity.START
            val d=parent.resources.displayMetrics.density
            binding.thumb.layoutParams=android.widget.LinearLayout.LayoutParams(-1,(120*d).toInt())
            (binding.title.parent as android.widget.LinearLayout).layoutParams=android.widget.LinearLayout.LayoutParams(-1,-2)
            root.setPadding((10*d).toInt(),(10*d).toInt(),(10*d).toInt(),(10*d).toInt())
        }
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(
        holder: VH,
        position: Int,
    ) {
        val song = items[position]
        val b = holder.b
        b.title.text = song.title
        b.subtitle.text = song.artist
        b.duration.text = if (song.live) "LIVE" else song.duration

        val color =
            if (song.id == playingId) {
                ContextCompat.getColor(b.root.context, R.color.brand_primary)
            } else {
                MaterialColors.getColor(b.title, com.google.android.material.R.attr.colorOnSurface)
            }
        b.title.setTextColor(color)

        ImageLoader.load(song.thumb, b.thumb, R.drawable.ic_placeholder)
        b.thumb.background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = 10 * b.root.resources.displayMetrics.density; setColor(android.graphics.Color.TRANSPARENT) }
        b.thumb.clipToOutline = true
        b.songMore.contentDescription = "Actions for ${song.title}"
        b.songMore.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onLongClick(pos)
        }
        b.root.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onLongClick(pos)
            true
        }
        b.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onClick(pos)
        }
    }
}
