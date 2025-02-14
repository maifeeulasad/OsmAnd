package net.osmand.plus.widgets.cmadapter.callback;

import android.widget.ArrayAdapter;

import net.osmand.plus.widgets.cmadapter.item.ContextMenuItem;

public interface ItemLongClickListener {
	/**
	 * @return true if drawer should be closed
	 */
	boolean onContextMenuLongClick(ArrayAdapter<ContextMenuItem> adapter,
	                               int itemId, int position, boolean isChecked,
	                               int[] viewCoordinates);
}
