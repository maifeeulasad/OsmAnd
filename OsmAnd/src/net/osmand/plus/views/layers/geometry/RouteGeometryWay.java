package net.osmand.plus.views.layers.geometry;

import android.graphics.Paint;

import net.osmand.Location;
import net.osmand.core.jni.QListFColorARGB;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.util.Algorithms;

import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RouteGeometryWay extends
		MultiColoringGeometryWay<RouteGeometryWayContext, MultiColoringGeometryWayDrawer<RouteGeometryWayContext>> {

	private final RoutingHelper helper;
	private RouteCalculationResult route;

	private Integer customDirectionArrowColor;

	public RouteGeometryWay(RouteGeometryWayContext context) {
		super(context, new MultiColoringGeometryWayDrawer<>(context));
		this.helper = context.getApp().getRoutingHelper();
	}

	public void setRouteStyleParams(int pathColor,
	                                float pathWidth,
	                                @Nullable @ColorInt Integer directionArrowColor,
	                                @NonNull ColoringType routeColoringType,
	                                @Nullable String routeInfoAttribute) {
		this.coloringChanged = this.coloringType != routeColoringType
				|| routeColoringType == ColoringType.ATTRIBUTE
				&& !Algorithms.objectEquals(this.routeInfoAttribute, routeInfoAttribute);

		boolean widthChanged = !Algorithms.objectEquals(customWidth, pathWidth);
		if (widthChanged) {
			updateStylesWidth(pathWidth);
		}
		updatePaints(pathWidth, routeColoringType);
		getDrawer().setColoringType(routeColoringType);

		this.customColor = pathColor;
		this.customWidth = pathWidth;
		this.customDirectionArrowColor = directionArrowColor;
		this.coloringType = routeColoringType;
		this.routeInfoAttribute = routeInfoAttribute;
	}

	@Override
	protected void updatePaints(@Nullable Float width, @NonNull ColoringType routeColoringType) {
		super.updatePaints(width, routeColoringType);
		Paint.Cap cap = routeColoringType.isGradient() || routeColoringType.isRouteInfoAttribute() ?
				Paint.Cap.ROUND : getContext().getAttrs().paint.getStrokeCap();
		getContext().getAttrs().customColorPaint.setStrokeCap(cap);
	}

	public void updateRoute(@NonNull RotatedTileBox tb, @NonNull RouteCalculationResult route) {
		if (coloringChanged || tb.getMapDensity() != getMapDensity() || this.route != route) {
			this.route = route;
			coloringChanged = false;
			List<Location> locations = route.getImmutableAllLocations();

			if (coloringType.isGradient()) {
				updateGradientRoute(tb, locations);
			} else if (coloringType.isRouteInfoAttribute()) {
				updateSolidMultiColorRoute(tb, locations, route.getOriginalRoute());
			} else {
				updateWay(locations, tb);
			}
		}
	}

	@NonNull
	@Override
	public GeometryWayStyle<?> getDefaultWayStyle() {
		return coloringType.isGradient()
				? super.getGradientWayStyle()
				: getArrowWayStyle(customColor);
	}

	@NonNull
	@Override
	public GeometrySolidWayStyle<RouteGeometryWayContext> getSolidWayStyle(int lineColor) {
		return getArrowWayStyle(lineColor);
	}

	@NonNull
	private GeometrySolidWayStyle<RouteGeometryWayContext> getArrowWayStyle(int lineColor) {
		int directionArrowColor = customDirectionArrowColor != null
				? customDirectionArrowColor
				: getContext().getPaintIcon().getColor();
		return new GeometrySolidWayStyle<>(getContext(), lineColor, customWidth, directionArrowColor, true);
	}

	@Override
	protected PathGeometryZoom getGeometryZoom(RotatedTileBox tb) {
		if (coloringType.isGradient()) {
			int zoom = tb.getZoom();
			PathGeometryZoom zm = zooms.get(zoom);
			if (zm == null) {
				zm = new GradientPathGeometryZoom(getLocationProvider(), tb, true);
				zooms.put(zoom, zm);
			}
			return zm;
		}
		return super.getGeometryZoom(tb);
	}

	@Override
	public Location getNextVisiblePoint() {
		return helper.getRoute().getCurrentStraightAnglePoint();
	}

	public void clearRoute() {
		if (route != null) {
			route = null;
			clearWay();
		}
	}

	public QListFColorARGB getColorizationMapping() {
		//OpenGL
		QListFColorARGB colors = new QListFColorARGB();
		if (styleMap != null && styleMap.size() > 0) {
			int lastColor = 0;
			for (int i = 0; i < styleMap.size(); i++) {
				GeometryWayStyle<?> style = styleMap.get(i);
				int color = 0;
				if (style != null) {
					if (style instanceof GeometryGradientWayStyle) {
						color = ((GeometryGradientWayStyle) style).currColor;
						lastColor = ((GeometryGradientWayStyle) style).nextColor;
					} else {
						color = style.getColor() == null ? 0 : style.getColor();
						lastColor = color;
					}
				}
				colors.add(NativeUtilities.createFColorARGB(color));
			}
			colors.add(NativeUtilities.createFColorARGB(lastColor));
		}
		return colors;
	}

	public int getColorizationScheme() {
		//OpenGL
		if (styleMap != null) {
			for (int i = 0; i < styleMap.size(); i++) {
				GeometryWayStyle<?> style = styleMap.get(i);
				if (style != null) {
					return style.getColorizationScheme();
				}
			}
		}
		return GeometryWayStyle.COLORIZATION_NONE;
	}
}