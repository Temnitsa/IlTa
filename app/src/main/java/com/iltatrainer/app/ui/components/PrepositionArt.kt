package com.iltatrainer.app.ui.components

import com.iltatrainer.app.R

/**
 * Maps dictionary keys (`pos_above`, `dir_away_from`, …) to the generated picture resources
 * in `res/drawable-nodpi/prep_<key>.png`.
 */
object PrepositionArt {

    fun drawableFor(key: String): Int? = when (key) {
        // Positional prepositions.
        "pos_above" -> R.drawable.prep_pos_above
        "pos_across" -> R.drawable.prep_pos_across
        "pos_against" -> R.drawable.prep_pos_against
        "pos_among" -> R.drawable.prep_pos_among
        "pos_around" -> R.drawable.prep_pos_around
        "pos_behind" -> R.drawable.prep_pos_behind
        "pos_below" -> R.drawable.prep_pos_below
        "pos_beneath" -> R.drawable.prep_pos_beneath
        "pos_beside" -> R.drawable.prep_pos_beside
        "pos_between" -> R.drawable.prep_pos_between
        "pos_by" -> R.drawable.prep_pos_by
        "pos_in" -> R.drawable.prep_pos_in
        "pos_in_front_of" -> R.drawable.prep_pos_in_front_of
        "pos_inside" -> R.drawable.prep_pos_inside
        "pos_near" -> R.drawable.prep_pos_near
        "pos_next_to" -> R.drawable.prep_pos_next_to
        "pos_on" -> R.drawable.prep_pos_on
        "pos_outside" -> R.drawable.prep_pos_outside
        "pos_over" -> R.drawable.prep_pos_over
        "pos_through" -> R.drawable.prep_pos_through
        "pos_under" -> R.drawable.prep_pos_under
        "pos_underneath" -> R.drawable.prep_pos_underneath

        // Prepositions of direction.
        "dir_across" -> R.drawable.prep_dir_across
        "dir_against" -> R.drawable.prep_dir_against
        "dir_along" -> R.drawable.prep_dir_along
        "dir_around" -> R.drawable.prep_dir_around
        "dir_away_from" -> R.drawable.prep_dir_away_from
        "dir_down" -> R.drawable.prep_dir_down
        "dir_from" -> R.drawable.prep_dir_from
        "dir_in_to" -> R.drawable.prep_dir_in_to
        "dir_into" -> R.drawable.prep_dir_into
        "dir_off_of" -> R.drawable.prep_dir_off_of
        "dir_on_to" -> R.drawable.prep_dir_on_to
        "dir_onto" -> R.drawable.prep_dir_onto
        "dir_out_of" -> R.drawable.prep_dir_out_of
        "dir_over" -> R.drawable.prep_dir_over
        "dir_through" -> R.drawable.prep_dir_through
        "dir_to" -> R.drawable.prep_dir_to
        "dir_towards" -> R.drawable.prep_dir_towards
        "dir_under" -> R.drawable.prep_dir_under
        "dir_up" -> R.drawable.prep_dir_up

        else -> null
    }
}
