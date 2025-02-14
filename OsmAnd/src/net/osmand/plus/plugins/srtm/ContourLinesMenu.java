package net.osmand.plus.plugins.srtm;

import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.SelectIndexesHelper;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.cmadapter.ContextMenuAdapter;
import net.osmand.plus.widgets.cmadapter.item.ContextMenuCategory;
import net.osmand.plus.widgets.cmadapter.item.ContextMenuItem;
import net.osmand.plus.widgets.cmadapter.callback.ItemClickListener;
import net.osmand.plus.widgets.cmadapter.callback.OnRowItemClick;
import net.osmand.plus.widgets.cmadapter.callback.ProgressListener;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.text.DateFormat;
import java.util.List;

import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_DENSITY_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_LINES_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_LINES_DISABLED_VALUE;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_LINES_SCHEME_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_WIDTH_ATTR;

public class ContourLinesMenu {

	private static final Log LOG = PlatformUtil.getLog(ContourLinesMenu.class);
	private static final String TAG = "ContourLinesMenu";

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		OsmandPlugin.enablePluginIfNeeded(mapActivity, mapActivity.getMyApplication(), plugin, true);
		boolean nightMode = isNightMode(mapActivity.getMyApplication());
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity.getMyApplication());
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		adapter.setProfileDependent(true);
		createLayersItems(adapter, mapActivity);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter,
										  final MapActivity mapActivity) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		final boolean srtmEnabled = OsmandPlugin.isActive(SRTMPlugin.class) || InAppPurchaseHelper.isContourLinesPurchased(app);

		final RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		final RenderingRuleProperty colorSchemeProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_SCHEME_ATTR);
		if (plugin == null || contourLinesProp == null || colorSchemeProp == null) {
			return;
		}

		final String contourWidthName;
		final String contourDensityName;
		final CommonPreference<String> widthPref;
		final CommonPreference<String> densityPref;
		final RenderingRuleProperty contourWidthProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_WIDTH_ATTR);
		if (contourWidthProp != null) {
			contourWidthName = AndroidUtils.getRenderingStringPropertyName(app, contourWidthProp.getAttrName(),
					contourWidthProp.getName());
			widthPref = settings.getCustomRenderProperty(contourWidthProp.getAttrName());
		} else {
			contourWidthName = null;
			widthPref = null;
		}
		final RenderingRuleProperty contourDensityProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_DENSITY_ATTR);
		if (contourDensityProp != null) {
			contourDensityName = AndroidUtils.getRenderingStringPropertyName(app, contourDensityProp.getAttrName(),
					contourDensityProp.getName());
			densityPref = settings.getCustomRenderProperty(contourDensityProp.getAttrName());
		} else {
			contourDensityName = null;
			densityPref = null;
		}

		final CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
		final CommonPreference<String> colorPref = settings.getCustomRenderProperty(colorSchemeProp.getAttrName());

		final boolean selected = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);
		final int toggleActionStringId = selected ? R.string.shared_string_on : R.string.shared_string_off;
		final int showZoomLevelStringId = R.string.show_from_zoom_level;
		final int colorSchemeStringId = R.string.srtm_color_scheme;

		OnRowItemClick l = new OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter,
										  View view, int itemId, int pos) {
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
											  final int itemId, final int pos, final boolean isChecked, int[] viewCoordinates) {
				if (itemId == toggleActionStringId) {
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							plugin.toggleContourLines(mapActivity, isChecked, new Runnable() {
								@Override
								public void run() {
									mapActivity.getDashboard().refreshContent(true);
									mapActivity.refreshMapComplete();
								}
							});
						}
					});
				} else if (itemId == showZoomLevelStringId) {
					plugin.selectPropertyValue(mapActivity, contourLinesProp, pref, new Runnable() {
						@Override
						public void run() {
							ContextMenuItem item = adapter.getItem(pos);
							if (item != null) {
								item.setDescription(plugin.getPrefDescription(app, contourLinesProp, pref));
								adapter.notifyDataSetChanged();
							}
							mapActivity.refreshMapComplete();
						}
					});
				} else if (itemId == colorSchemeStringId) {
					plugin.selectPropertyValue(mapActivity, colorSchemeProp, colorPref, new Runnable() {
						@Override
						public void run() {
							ContextMenuItem item = adapter.getItem(pos);
							if (item != null) {
								item.setDescription(plugin.getPrefDescription(app, colorSchemeProp, colorPref));
								adapter.notifyDataSetChanged();
							}
							mapActivity.refreshMapComplete();
						}
					});
				} else if (itemId == R.string.srtm_plugin_name) {
					ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.TERRAIN);
					closeDashboard(mapActivity);
				} else if (contourWidthProp != null && itemId == contourWidthName.hashCode()) {
					plugin.selectPropertyValue(mapActivity, contourWidthProp, widthPref, new Runnable() {
						@Override
						public void run() {
							ContextMenuItem item = adapter.getItem(pos);
							if (item != null) {
								item.setDescription(plugin.getPrefDescription(app, contourWidthProp, widthPref));
								adapter.notifyDataSetChanged();
							}
							mapActivity.refreshMapComplete();
						}
					});
				} else if (contourDensityProp != null && itemId == contourDensityName.hashCode()) {
					plugin.selectPropertyValue(mapActivity, contourDensityProp, densityPref, new Runnable() {
						@Override
						public void run() {
							ContextMenuItem item = adapter.getItem(pos);
							if (item != null) {
								item.setDescription(plugin.getPrefDescription(app, contourDensityProp, densityPref));
								adapter.notifyDataSetChanged();
							}
							mapActivity.refreshMapComplete();
						}
					});
				}
				return false;
			}
		};

		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		int toggleIconColorId;
		int toggleIconId;
		if (selected) {
			toggleIconId = R.drawable.ic_action_view;
			toggleIconColorId = ColorUtilities.getActiveColorId(nightMode);
		} else {
			toggleIconId = R.drawable.ic_action_hide;
			toggleIconColorId = ContextMenuItem.INVALID_ID;
		}
		contextMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(toggleActionStringId, mapActivity)
				.setIcon(toggleIconId)
				.setColor(app, toggleIconColorId)
				.setListener(l)
				.setSelected(selected));
		if (selected) {
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(showZoomLevelStringId, mapActivity)
					.setLayout(R.layout.list_item_single_line_descrition_narrow)
					.setIcon(R.drawable.ic_action_map_magnifier)
					.setDescription(plugin.getPrefDescription(app, contourLinesProp, pref))
					.setListener(l));
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(colorSchemeStringId, mapActivity)
					.setLayout(R.layout.list_item_single_line_descrition_narrow)
					.setIcon(R.drawable.ic_action_appearance)
					.setDescription(plugin.getPrefDescription(app, colorSchemeProp, colorPref))
					.setListener(l));
			if (contourWidthProp != null) {
				contextMenuAdapter.addItem(new ContextMenuItem(null)
						.setTitle(contourWidthName)
						.setLayout(R.layout.list_item_single_line_descrition_narrow)
						.setIcon(R.drawable.ic_action_gpx_width_thin)
						.setDescription(plugin.getPrefDescription(app, contourWidthProp, widthPref))
						.setListener(l));
			}
			if (contourDensityProp != null) {
				contextMenuAdapter.addItem(new ContextMenuItem(null)
						.setTitle(contourDensityName)
						.setLayout(R.layout.list_item_single_line_descrition_narrow)
						.setIcon(R.drawable.ic_plugin_srtm)
						.setDescription(plugin.getPrefDescription(app, contourDensityProp, densityPref))
						.setListener(l));
			}
		}

		if (!srtmEnabled) {
			contextMenuAdapter.addItem(new ContextMenuCategory(null)
					.setTitleId(R.string.srtm_purchase_header, mapActivity)
					.setLayout(R.layout.list_group_title_with_switch_light));
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(R.string.srtm_plugin_name, mapActivity)
					.setLayout(R.layout.list_item_icon_and_right_btn)
					.setIcon(R.drawable.ic_plugin_srtm)
					.setColor(app, R.color.osmand_orange)
					.setDescription(app.getString(R.string.shared_string_plugin))
					.setListener(l));
		} else {
			final DownloadIndexesThread downloadThread = app.getDownloadThread();
			if (!downloadThread.getIndexes().isDownloadedFromInternet) {
				if (settings.isInternetConnectionAvailable()) {
					downloadThread.runReloadIndexFiles();
				}
			}

			if (downloadThread.shouldDownloadIndexes()) {
				contextMenuAdapter.addItem(createDownloadSrtmMapsItem(mapActivity));
				contextMenuAdapter.addItem(new ContextMenuItem(null)
						.setLayout(R.layout.list_item_icon_and_download)
						.setTitleId(R.string.downloading_list_indexes, mapActivity)
						.setLoading(true)
						.setListener(l));
			} else {
				try {
					List<IndexItem> srtms = DownloadResources.findIndexItemsAt(
							app, mapActivity.getMapLocation(), DownloadActivityType.SRTM_COUNTRY_FILE,
							false, 1, true);
					SrtmDownloadItem srtmDownloadItem = convertToSrtmDownloadItem(srtms);
					if (srtmDownloadItem != null) {
						contextMenuAdapter.addItem(createDownloadSrtmMapsItem(mapActivity));
						contextMenuAdapter.addItem(createSrtmDownloadItem(mapActivity, srtmDownloadItem));
					}
				} catch (IOException e) {
					LOG.error(e);
				}
			}
		}

		contextMenuAdapter.addItem(new ContextMenuItem(null)
				.setLayout(R.layout.card_bottom_divider)
				);
	}

	private static ContextMenuItem createDownloadSrtmMapsItem(MapActivity mapActivity) {
		return new ContextMenuCategory(null)
				.setTitleId(R.string.shared_string_download_map, mapActivity)
				.setDescription(mapActivity.getString(R.string.srtm_menu_download_descr))
				.setLayout(R.layout.list_group_title_with_descr);
	}

	@Nullable
	private static SrtmDownloadItem convertToSrtmDownloadItem(List<IndexItem> srtms) {
		if (Algorithms.isEmpty(srtms)) {
			return null;
		}
		List<DownloadItem> individualResources = srtms.get(0).getRelatedGroup().getIndividualDownloadItems();
		for (DownloadItem downloadItem : individualResources) {
			if (downloadItem instanceof SrtmDownloadItem) {
				return (SrtmDownloadItem) downloadItem;
			}
		}
		return null;
	}

	private static ContextMenuItem createSrtmDownloadItem(MapActivity mapActivity, SrtmDownloadItem srtmDownloadItem) {
		OsmandApplication app = mapActivity.getMyApplication();
		DownloadIndexesThread downloadThread = app.getDownloadThread();

		ContextMenuItem item = new ContextMenuItem(null)
				.setLayout(R.layout.list_item_icon_and_download)
				.setTitle(srtmDownloadItem.getVisibleName(app, app.getRegions(), false))
				.setDescription(DownloadActivityType.SRTM_COUNTRY_FILE.getString(app))
				.hideDivider(true)
				.setIcon(DownloadActivityType.SRTM_COUNTRY_FILE.getIconResource())
				.setListener(getOnSrtmItemClickListener(mapActivity, srtmDownloadItem))
				.setProgressListener(getSrtmItemProgressListener(srtmDownloadItem, downloadThread));

		if (srtmDownloadItem.isCurrentlyDownloading(downloadThread)) {
			item.setLoading(true)
					.setProgress(downloadThread.getCurrentDownloadingItemProgress())
					.setSecondaryIcon(R.drawable.ic_action_remove_dark);
		} else {
			item.setSecondaryIcon(R.drawable.ic_action_import);
		}

		return item;
	}

	private static ItemClickListener getOnSrtmItemClickListener(MapActivity mapActivity,
	                                                            SrtmDownloadItem srtmDownloadItem) {
		return (adapter, itemId, position, isChecked, viewCoordinates) -> {

			OsmandApplication app = mapActivity.getMyApplication();
			DownloadIndexesThread downloadThread = app.getDownloadThread();
			ContextMenuItem item = adapter.getItem(position);
			IndexItem indexItem = srtmDownloadItem.getIndexItem(downloadThread);

			if (downloadThread.isDownloading(indexItem)) {
				downloadThread.cancelDownload(indexItem);
				if (item != null) {
					item.setProgress(ContextMenuItem.INVALID_ID);
					item.setLoading(false);
					item.setSecondaryIcon(R.drawable.ic_action_import);
					adapter.notifyDataSetChanged();
				}
			} else {
				DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(mapActivity);
				SelectIndexesHelper.showDialog(srtmDownloadItem, mapActivity, dateFormat, true, items -> {
					IndexItem[] toDownload = new IndexItem[items.size()];
					new DownloadValidationManager(app).startDownload(mapActivity, items.toArray(toDownload));
					if (item != null) {
						item.setProgress(ContextMenuItem.INVALID_ID);
						item.setLoading(true);
						item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
						adapter.notifyDataSetChanged();
					}
				});
			}

			return false;
		};
	}

	private static ProgressListener getSrtmItemProgressListener(SrtmDownloadItem srtmDownloadItem,
	                                                            DownloadIndexesThread downloadThread) {
		return (progressObject, progress, adapter, itemId, position) -> {
			if (progressObject instanceof IndexItem) {
				IndexItem progressItem = (IndexItem) progressObject;
				if (srtmDownloadItem.getIndexItem(downloadThread).compareTo(progressItem) == 0) {
					ContextMenuItem item = adapter.getItem(position);
					if (item != null) {
						item.setProgress(progress);
						item.setLoading(true);
						item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
						adapter.notifyDataSetChanged();
					}
					return true;
				}
			}
			return false;
		};
	}

	public static boolean isNightMode(OsmandApplication app) {
		if (app == null) {
			return false;
		}
		return app.getDaynightHelper().isNightModeForMapControls();
	}

	public static void closeDashboard(MapActivity mapActivity) {
		mapActivity.getDashboard().hideDashboard(false);
	}
}
