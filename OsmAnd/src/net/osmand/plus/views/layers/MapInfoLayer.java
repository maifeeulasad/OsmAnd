package net.osmand.plus.views.layers;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.TrackChartPoints;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.LanesControl;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopCoordinatesView;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopTextView;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarView;
import net.osmand.plus.views.mapwidgets.MapMarkersWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapWidgetRegInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget;
import net.osmand.plus.views.mapwidgets.widgets.ElevationProfileWidget;
import net.osmand.plus.views.mapwidgets.widgets.NextTurnWidget;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.BearingWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.CompassRulerWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.TimeWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_ALTITUDE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_BATTERY;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_BEARING;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_COMPASS;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_DISTANCE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_GPS_INFO;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_INTERMEDIATE_DISTANCE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_INTERMEDIATE_TIME;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MARKER_1;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MARKER_2;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MAX_SPEED;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_TURN_SMALL;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_PLAIN_TIME;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_RADIUS_RULER;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_SPEED;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_TIME;

public class MapInfoLayer extends OsmandMapLayer {

	private static boolean WIDGETS_EXPANDED = false;

	private final RouteLayer routeLayer;
	private final OsmandSettings settings;
	private OsmandMapTileView view;

	// groups
	private LinearLayout rightStack;
	private LinearLayout leftStack;
	private ImageButton expandButton;
	private View mapRulerLayout;
	private LanesControl lanesControl;
	private AlarmWidget alarmControl;
	private List<RulerWidget> rulerWidgets;
	private MapWidgetRegistry mapInfoControls;

	private DrawSettings drawSettings;
	private int themeId = -1;

	private TopTextView streetNameView;
	private TopToolbarView topToolbarView;
	private TopCoordinatesView topCoordinatesView;
	private ElevationProfileWidget elevationProfileWidget;

	public MapInfoLayer(@NonNull Context context, @NonNull RouteLayer layer) {
		super(context);
		settings = getApplication().getSettings();
		this.routeLayer = layer;
	}

	public MapWidgetRegistry getMapInfoControls() {
		return mapInfoControls;
	}

	@Override
	public void initLayer(@NonNull final OsmandMapTileView view) {
		this.view = view;
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			mapInfoControls = mapActivity.getMapLayers().getMapWidgetRegistry();
			leftStack = mapActivity.findViewById(R.id.map_left_widgets_panel);
			rightStack = mapActivity.findViewById(R.id.map_right_widgets_panel);
			expandButton = mapActivity.findViewById(R.id.map_collapse_button);
			mapRulerLayout = mapActivity.findViewById(R.id.map_ruler_layout);

			// update and create controls
			registerAllControls(mapActivity);
			getApplication().getAidlApi().registerWidgetControls(mapActivity);

			recreateControls();
		} else {
			if (mapInfoControls != null) {
				mapInfoControls.clearSideWidgets();
			}
			mapInfoControls = null;

			leftStack = null;
			rightStack = null;
			expandButton = null;
			mapRulerLayout = null;

			lanesControl = null;
			alarmControl = null;
			rulerWidgets = null;

			drawSettings = null;
			themeId = -1;

			streetNameView = null;
			topToolbarView = null;
			topCoordinatesView = null;
		}
	}

	@Nullable
	public MapWidgetRegInfo registerSideWidget(TextInfoWidget widget, int drawableMenu,
											   int messageId, String key, boolean left, int priorityOrder) {
		if (mapInfoControls != null) {
			MapWidgetRegInfo reg = mapInfoControls.registerSideWidgetInternal(widget, drawableMenu, messageId, key, left, priorityOrder);
			updateReg(calculateTextState(), reg);
			return reg;
		} else {
			return null;
		}
	}

	@Nullable
	public MapWidgetRegInfo registerSideWidget(TextInfoWidget widget, int drawableMenu,
											   String message, String key, boolean left, int priorityOrder) {
		if (mapInfoControls != null) {
			MapWidgetRegInfo reg = mapInfoControls.registerSideWidgetInternal(widget, drawableMenu, message, key, left, priorityOrder);
			updateReg(calculateTextState(), reg);
			return reg;
		} else {
			return null;
		}
	}

	public void registerSideWidget(TextInfoWidget widget, WidgetState widgetState, String key, boolean left, int priorityOrder) {
		if (mapInfoControls != null) {
			MapWidgetRegInfo reg = mapInfoControls.registerSideWidgetInternal(widget, widgetState, key, left, priorityOrder);
			updateReg(calculateTextState(), reg);
		}
	}

	@Nullable
	public <T extends TextInfoWidget> T getSideWidget(Class<T> cl) {
		return mapInfoControls != null ? mapInfoControls.getSideWidget(cl) : null;
	}

	public void removeSideWidget(TextInfoWidget widget) {
		if (mapInfoControls != null) {
			mapInfoControls.removeSideWidgetInternal(widget);
		}
	}

	public void addTopToolbarController(TopToolbarController controller) {
		if (topToolbarView != null) {
			topToolbarView.addController(controller);
		}
	}

	public void removeTopToolbarController(TopToolbarController controller) {
		if (topToolbarView != null) {
			topToolbarView.removeController(controller);
		}
	}

	public boolean hasTopToolbar() {
		return topToolbarView != null && topToolbarView.getTopController() != null;
	}

	public TopToolbarController getTopToolbarController() {
		return topToolbarView == null ? null : topToolbarView.getTopController();
	}

	@Nullable
	public TopToolbarController getTopToolbarController(TopToolbarControllerType type) {
		return topToolbarView == null ? null : topToolbarView.getController(type);
	}

	public boolean isTopToolbarViewVisible() {
		return topToolbarView != null && topToolbarView.isTopToolbarViewVisible();
	}

	private void registerAllControls(@NonNull MapActivity map) {
		rulerWidgets = new ArrayList<>();
		RouteInfoWidgetsFactory ric = new RouteInfoWidgetsFactory();
		MapInfoWidgetsFactory mic = new MapInfoWidgetsFactory();
		MapMarkersWidgetsFactory mwf = map.getMapLayers().getMapMarkersLayer().getWidgetsFactory();
		OsmandApplication app = view.getApplication();
		lanesControl = RouteInfoWidgetsFactory.createLanesControl(map, view);

		TextState ts = calculateTextState();
		streetNameView = new TopTextView(map.getMyApplication(), map);
		updateStreetName(false, ts);

		topCoordinatesView = new TopCoordinatesView(map.getMyApplication(), map);
		updateTopCoordinates(false, ts);

		topToolbarView = new TopToolbarView(map);
		updateTopToolbar(false);

		alarmControl = RouteInfoWidgetsFactory.createAlarmInfoControl(app, map);
		alarmControl.setVisibility(false);

		elevationProfileWidget = new ElevationProfileWidget(map);

		setupRulerWidget(mapRulerLayout);

		// register left stack
		registerSideWidget(null, R.drawable.ic_action_compass, R.string.map_widget_compass, WIDGET_COMPASS, true, 4);

		NextTurnWidget bigInfoControl = ric.createNextInfoControl(map, app, false);
		registerSideWidget(bigInfoControl, R.drawable.ic_action_next_turn, R.string.map_widget_next_turn, WIDGET_NEXT_TURN, true, 5);
		NextTurnWidget smallInfoControl = ric.createNextInfoControl(map, app, true);
		registerSideWidget(smallInfoControl, R.drawable.ic_action_next_turn, R.string.map_widget_next_turn_small, WIDGET_NEXT_TURN_SMALL, true, 6);
		NextTurnWidget nextNextInfoControl = ric.createNextNextInfoControl(map, app, true);
		registerSideWidget(nextNextInfoControl, R.drawable.ic_action_next_turn, R.string.map_widget_next_next_turn, WIDGET_NEXT_NEXT_TURN, true, 7);

		// register right stack
		// priorityOrder: 10s navigation-related, 20s position-related, 30s recording- and other plugin-related, 40s general device information, 50s debugging-purpose
		TextInfoWidget intermediateDist = ric.createIntermediateDistanceControl(map);
		registerSideWidget(intermediateDist, R.drawable.ic_action_intermediate, R.string.map_widget_intermediate_distance, WIDGET_INTERMEDIATE_DISTANCE, false, 13);
		TextInfoWidget intermediateTime = ric.createTimeControl(map, true);
		registerSideWidget(intermediateTime, new TimeWidgetState(app, true), WIDGET_INTERMEDIATE_TIME, false, 14);
		TextInfoWidget dist = ric.createDistanceControl(map);
		registerSideWidget(dist, R.drawable.ic_action_target, R.string.map_widget_distance, WIDGET_DISTANCE, false, 15);
		TextInfoWidget time = ric.createTimeControl(map, false);
		registerSideWidget(time, new TimeWidgetState(app, false), WIDGET_TIME, false, 16);


		TextInfoWidget marker = mwf.createMapMarkerControl(map, true);
		registerSideWidget(marker, R.drawable.ic_action_flag, R.string.map_marker_1st, WIDGET_MARKER_1, false, 17);
		TextInfoWidget bearing = ric.createBearingControl(map);
		registerSideWidget(bearing, new BearingWidgetState(app), WIDGET_BEARING, false, 18);
		TextInfoWidget marker2nd = mwf.createMapMarkerControl(map, false);
		registerSideWidget(marker2nd, R.drawable.ic_action_flag, R.string.map_marker_2nd, WIDGET_MARKER_2, false, 19);

		TextInfoWidget speed = ric.createSpeedControl(map);
		registerSideWidget(speed, R.drawable.ic_action_speed, R.string.map_widget_speed, WIDGET_SPEED, false, 20);
		TextInfoWidget maxspeed = ric.createMaxSpeedControl(map);
		registerSideWidget(maxspeed, R.drawable.ic_action_speed_limit, R.string.map_widget_max_speed, WIDGET_MAX_SPEED, false, 21);
		TextInfoWidget alt = mic.createAltitudeControl(map);
		registerSideWidget(alt, R.drawable.ic_action_altitude, R.string.map_widget_altitude, WIDGET_ALTITUDE, false, 23);
		TextInfoWidget gpsInfo = mic.createGPSInfoControl(map);

		registerSideWidget(gpsInfo, R.drawable.ic_action_gps_info, R.string.map_widget_gps_info, WIDGET_GPS_INFO, false, 28);
		TextInfoWidget plainTime = ric.createPlainTimeControl(map);
		registerSideWidget(plainTime, R.drawable.ic_action_time, R.string.map_widget_plain_time, WIDGET_PLAIN_TIME, false, 41);
		TextInfoWidget battery = ric.createBatteryControl(map);
		registerSideWidget(battery, R.drawable.ic_action_battery, R.string.map_widget_battery, WIDGET_BATTERY, false, 42);
		TextInfoWidget radiusRuler = mic.createRadiusRulerControl(map);
		registerSideWidget(radiusRuler, new CompassRulerWidgetState(app), WIDGET_RADIUS_RULER, false, 43);
	}

	public void recreateControls() {
		if (leftStack != null) {
			leftStack.removeAllViews();
			if (mapInfoControls != null) {
				mapInfoControls.populateStackControl(leftStack, settings.getApplicationMode(), true, WIDGETS_EXPANDED);
			}
			leftStack.requestLayout();
		}
		if (rightStack != null) {
			rightStack.removeAllViews();
			if (mapInfoControls != null) {
				mapInfoControls.populateStackControl(rightStack, settings.getApplicationMode(), false, WIDGETS_EXPANDED);
			}
			rightStack.requestLayout();
		}
		if (expandButton != null) {
			expandButton.setVisibility(mapInfoControls.hasCollapsibles(settings.getApplicationMode()) ?
					View.VISIBLE : View.GONE);
			UiUtilities uiUtilities = getApplication().getUIUtilities();
			int iconId = WIDGETS_EXPANDED ? R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down;
			int colorId = ColorUtilities.getMapButtonIconColorId(false);
			Drawable expandIcon = uiUtilities.getIcon(iconId, colorId);
			setMapButtonIcon(expandButton, expandIcon);
			expandButton.setContentDescription(getString(WIDGETS_EXPANDED ? R.string.shared_string_collapse : R.string.access_widget_expand));
			expandButton.setOnClickListener(v -> {
				WIDGETS_EXPANDED = !WIDGETS_EXPANDED;
				recreateControls();
			});
		}
	}

	public RulerWidget setupRulerWidget(View mapRulerView) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RulerWidget rulerWidget = RouteInfoWidgetsFactory.createRulerControl(mapActivity, mapRulerView);
			rulerWidget.setVisibility(false);

			TextState ts = calculateTextState();
			boolean nightMode = drawSettings != null && drawSettings.isNightMode();
			rulerWidget.updateTextSize(nightMode, ts.textColor, ts.textShadowColor, (int) (2 * view.getDensity()));

			rulerWidgets.add(rulerWidget);

			return rulerWidget;
		} else {
			return null;
		}
	}

	public void removeRulerWidgets(List<RulerWidget> rulers) {
		List<RulerWidget> widgetList = new ArrayList<>(rulerWidgets);
		widgetList.removeAll(rulers);
		rulerWidgets = widgetList;
	}

	public void setTrackChartPoints(TrackChartPoints trackChartPoints) {
		routeLayer.setTrackChartPoints(trackChartPoints);
	}

	private static class TextState {
		boolean textBold;
		boolean night;
		int textColor;
		int textShadowColor;
		int boxTop;
		int rightRes;
		int leftRes;
		int expand;
		int boxFree;
		int textShadowRadius;
	}

	public void updateColorShadowsOfText() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings != null && drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		int calcThemeId = (transparent ? 4 : 0) | (nightMode ? 2 : 0) | (following ? 1 : 0);
		if (themeId != calcThemeId) {
			themeId = calcThemeId;
			TextState ts = calculateTextState();
			mapActivity.findViewById(R.id.map_center_info).setBackgroundResource(ts.boxFree);
			if (mapInfoControls != null) {
				for (MapWidgetRegInfo reg : mapInfoControls.getLeftWidgetSet()) {
					updateReg(ts, reg);
				}
				for (MapWidgetRegInfo reg : mapInfoControls.getRightWidgetSet()) {
					updateReg(ts, reg);
				}
			}
			updateStreetName(nightMode, ts);
			updateTopCoordinates(nightMode, ts);
			updateTopToolbar(nightMode);
			lanesControl.updateTextSize(nightMode, ts.textColor, ts.textShadowColor, ts.textBold, ts.textShadowRadius / 2);
			int padding = expandButton.getPaddingLeft();
			expandButton.setBackgroundResource(ts.expand);
			expandButton.setPadding(padding, padding, padding, padding);
			rightStack.invalidate();
			leftStack.invalidate();

			for (RulerWidget rulerWidget : rulerWidgets) {
				rulerWidget.updateTextSize(nightMode, ts.textColor, ts.textShadowColor, (int) (2 * view.getDensity()));
			}
		}
	}

	private void updateStreetName(boolean nightMode, TextState ts) {
		MapActivity mapActivity = getMapActivity();
		boolean portrait = mapActivity != null && AndroidUiHelper.isOrientationPortrait(mapActivity);
		streetNameView.setBackgroundResource(portrait ? ts.boxTop : ts.boxFree);
		streetNameView.updateTextColor(nightMode, ts.textColor, ts.textShadowColor, ts.textBold, ts.textShadowRadius);
	}

	private void updateTopToolbar(boolean nightMode) {
		topToolbarView.updateColors(nightMode);
	}

	private void updateTopCoordinates(boolean nightMode, TextState ts) {
		topCoordinatesView.updateColors(nightMode, ts.textBold);
	}

	private void updateReg(TextState ts, MapWidgetRegInfo reg) {
		View v = reg.widget != null ? reg.widget.getView().findViewById(R.id.widget_bg) : null;
		if (v != null) {
			v.setBackgroundResource(reg.left ? ts.leftRes : ts.rightRes);
			reg.widget.updateTextColor(ts.textColor, ts.textShadowColor, ts.textBold, ts.textShadowRadius);
			reg.widget.updateIconMode(ts.night);
		}
	}

	private TextState calculateTextState() {
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings != null && drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		TextState ts = new TextState();
		ts.textBold = following;
		ts.night = nightMode;
		ts.textColor = nightMode ? ContextCompat.getColor(view.getContext(), R.color.widgettext_night) :
				ContextCompat.getColor(view.getContext(), R.color.widgettext_day);
		// Night shadowColor always use widgettext_shadow_night, same as widget background color for non-transparent
		ts.textShadowColor = nightMode ? ContextCompat.getColor(view.getContext(), R.color.widgettext_shadow_night) :
				ContextCompat.getColor(view.getContext(), R.color.widgettext_shadow_day);
		if (!transparent && !nightMode) {
			ts.textShadowRadius = 0;
		} else {
			ts.textShadowRadius = (int) (4 * view.getDensity());
		}
		if (transparent) {
			ts.boxTop = R.drawable.btn_flat_transparent;
			ts.rightRes = R.drawable.btn_left_round_transparent;
			ts.leftRes = R.drawable.btn_right_round_transparent;
			ts.expand = R.drawable.btn_inset_circle_transparent;
			ts.boxFree = R.drawable.btn_round_transparent;
		} else if (nightMode) {
			ts.boxTop = R.drawable.btn_flat_night;
			ts.rightRes = R.drawable.btn_left_round_night;
			ts.leftRes = R.drawable.btn_right_round_night;
			ts.expand = R.drawable.btn_inset_circle_night;
			ts.boxFree = R.drawable.btn_round_night;
		} else {
			ts.boxTop = R.drawable.btn_flat;
			ts.rightRes = R.drawable.btn_left_round;
			ts.leftRes = R.drawable.btn_right_round;
			ts.expand = R.drawable.btn_inset_circle;
			ts.boxFree = R.drawable.btn_round;
		}
		return ts;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		this.drawSettings = drawSettings;
		if (getMapActivity() != null) {
			// update data on draw
			updateColorShadowsOfText();
			if (mapInfoControls != null) {
				mapInfoControls.updateInfo(settings.getApplicationMode(), drawSettings, WIDGETS_EXPANDED);
			}
			streetNameView.updateInfo(drawSettings);
			topToolbarView.updateInfo();
			topCoordinatesView.updateInfo();
			elevationProfileWidget.updateInfo();
			alarmControl.updateInfo(drawSettings, false);
			lanesControl.updateInfo(drawSettings);

			for (RulerWidget rulerWidget : rulerWidgets) {
				rulerWidget.updateInfo(tileBox, drawSettings);
			}
		}
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
}