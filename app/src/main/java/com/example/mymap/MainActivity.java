package com.example.mymap;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.BikingRouteOverlay;
import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.IndoorRouteOverlay;
import com.baidu.mapapi.overlayutil.MassTransitRouteOverlay;
import com.baidu.mapapi.overlayutil.TransitRouteOverlay;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private MapView mMapView = null;
    private BaiduMap mBaiduMap = null;
    private LocationClient mLocationClient = null;
    private BaiduMap.OnMarkerClickListener onMarkerClickListener = null;
    private RoutePlanSearch mSearch = null;
    private LatLng mLocation = null;
    private String mLocationCity = null;
    private PoiSearch mPoiSearch = null;
    private SuggestionSearch mSuggestionSearch = null;
    private EditText editText = findViewById(R.id.editText);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(routePlanResultListener);
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(suggestionResultListener);
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(poiSearchResultListener);

        // 设置地图中心点为辽宁沈阳东北大学浑南校区五舍
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(41.6577396168, 123.4343104372)));
        // 调整缩放级别
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18));
        Toast.makeText(getApplicationContext(), "正在定位", Toast.LENGTH_SHORT).show();

        // 开启定位
        initLocation();
        // 设置单击事件
        initClick();
    }

    private void initLocation() {
        //定位初始化
        mLocationClient = new LocationClient(this);

        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);

        //设置locationClientOption
        mLocationClient.setLocOption(option);

        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();
    }

    private void initClick() {
        BaiduMap.OnMapClickListener onMapClickListener = new BaiduMap.OnMapClickListener() {
            /**
             * 地图单击事件回调函数
             *
             * @param point 点击的地理坐标
             */
            @Override
            public void onMapClick(LatLng point) {
            }

            /**
             * 地图内 Poi 单击事件回调函数
             *
             * @param mapPoi 点击的 poi 信息
             */
            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                showSearchButton(mapPoi.getPosition());
                return true;
            }
        };
        //设置地图单击事件监听
        mBaiduMap.setOnMapClickListener(onMapClickListener);

        onMarkerClickListener = new BaiduMap.OnMarkerClickListener() {
            /**
             * 地图 Marker 覆盖物点击事件监听函数
             * @param marker 被点击的 marker
             */
            @Override
            public boolean onMarkerClick(Marker marker) {
                return false;//是否捕获点击事件
            }
        };

        // 设置地图 Marker 覆盖物点击事件监听者,自3.4.0版本起可设置多个监听对象，停止监听时调用removeMarkerClickListener移除监听对象
        mBaiduMap.setOnMarkerClickListener(onMarkerClickListener);
    }

    private void showSearchButton(final LatLng location) {
        //用来构造InfoWindow的Button
        Button button = new Button(getApplicationContext());
        button.setText("到这去");

        //构造InfoWindow
        //-10 InfoWindow相对于point在y轴的偏移量
        InfoWindow mInfoWindow = new InfoWindow(button, location, -10);

        //使InfoWindow生效
        mBaiduMap.showInfoWindow(mInfoWindow);

        // 按钮点击监听
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBaiduMap.clear();

                PlanNode stNode;
                if (mLocation != null) stNode = PlanNode.withLocation(mLocation);
                else stNode = PlanNode.withLocation(new LatLng(41.6577396168, 123.4343104372));
                PlanNode enNode = PlanNode.withLocation(location);
                mSearch.walkingSearch((new WalkingRoutePlanOption())
                        .from(stNode)
                        .to(enNode));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mLocationClient.stop();
        mSearch.destroy();
        mSuggestionSearch.destroy();
        mPoiSearch.destroy();
        mBaiduMap.setMyLocationEnabled(false);
        //停止监听时移除监听对象
        mBaiduMap.removeMarkerClickListener(onMarkerClickListener);
        mMapView.onDestroy();
        mMapView = null;
    }

    // 定位监听器
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            double mLatitude = location.getLatitude();
            double mLongitude = location.getLongitude();
            if (mLatitude != Double.MIN_VALUE && mLongitude != Double.MIN_VALUE) {
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(location.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(location.getDirection()).latitude(mLatitude)
                        .longitude(mLongitude).build();
                mBaiduMap.setMyLocationData(locData);
                mLocation = new LatLng(mLatitude, mLongitude);
                mLocationCity = location.getCity();
            }
        }
    }

    // 路线规划监听
    OnGetRoutePlanResultListener routePlanResultListener = new OnGetRoutePlanResultListener() {
        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {
            if (walkingRouteResult == null) {
                return;
            } else if (walkingRouteResult.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(getApplicationContext(), "路线查找失败 " + walkingRouteResult.error,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            //创建WalkingRouteOverlay实例
            WalkingRouteOverlay overlay = new WalkingRouteOverlay(mBaiduMap);
            if (walkingRouteResult.getRouteLines().size() > 0) {
                //获取路径规划数据,(以返回的第一条数据为例)
                //为WalkingRouteOverlay实例设置路径数据
                overlay.setData(walkingRouteResult.getRouteLines().get(0));
                //在地图上绘制WalkingRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {
            //创建BikingRouteOverlay实例
            BikingRouteOverlay overlay = new BikingRouteOverlay(mBaiduMap);
            if (bikingRouteResult.getRouteLines().size() > 0) {
                //获取路径规划数据,(以返回的第一条路线为例）
                //为BikingRouteOverlay实例设置数据
                overlay.setData(bikingRouteResult.getRouteLines().get(0));
                //在地图上绘制BikingRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {
            //创建IndoorRouteOverlay实例
            IndoorRouteOverlay overlay = new IndoorRouteOverlay(mBaiduMap);
            if (indoorRouteResult.getRouteLines() != null && indoorRouteResult.getRouteLines().size() > 0) {
                //获取室内路径规划数据（以返回的第一条路线为例）
                //为IndoorRouteOverlay实例设置数据
                overlay.setData(indoorRouteResult.getRouteLines().get(0));
                //在地图上绘制IndoorRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {
            //创建DrivingRouteOverlay实例
            DrivingRouteOverlay overlay = new DrivingRouteOverlay(mBaiduMap);
            if (drivingRouteResult.getRouteLines().size() > 0) {
                //获取路径规划数据,(以返回的第一条路线为例）
                //为DrivingRouteOverlay实例设置数据
                overlay.setData(drivingRouteResult.getRouteLines().get(0));
                //在地图上绘制DrivingRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {
            //创建MassTransitRouteOverlay实例
            MassTransitRouteOverlay overlay = new MassTransitRouteOverlay(mBaiduMap);
            if (massTransitRouteResult.getRouteLines() != null && massTransitRouteResult.getRouteLines().size() > 0) {
                //获取路线规划数据（以返回的第一条数据为例）
                //为MassTransitRouteOverlay设置数据
                overlay.setData(massTransitRouteResult.getRouteLines().get(0));
                //在地图上绘制Overlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {
            //创建TransitRouteOverlay实例
            TransitRouteOverlay overlay = new TransitRouteOverlay(mBaiduMap);
            //获取路径规划数据,(以返回的第一条数据为例)
            //为TransitRouteOverlay实例设置路径数据
            if (transitRouteResult.getRouteLines().size() > 0) {
                overlay.setData(transitRouteResult.getRouteLines().get(0));
                //在地图上绘制TransitRouteOverlay
                overlay.addToMap();
            }
        }
    };

    // 输入框监听
    class EditChangedListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String city;
            if (mLocation == null) city = "沈阳";
            else city = mLocationCity;
            mSuggestionSearch.requestSuggestion(new SuggestionSearchOption()
                    .city(city)
                    .keyword(editText.getText().toString()));
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }


    // 搜索关键词监听
    OnGetSuggestionResultListener suggestionResultListener = new OnGetSuggestionResultListener() {
        @Override
        public void onGetSuggestionResult(final SuggestionResult suggestionResult) {
            if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
                return;
            }
            final List<String> suggest = new ArrayList<>();
            List<SuggestionResult.SuggestionInfo> all_suggestion = suggestionResult.getAllSuggestions();
            if (all_suggestion.size() > 5) {
                all_suggestion = all_suggestion.subList(0, 5);
            }
            for (SuggestionResult.SuggestionInfo suggestionInfo : all_suggestion) {
                if (suggestionInfo.key != null) {
                    suggest.add(suggestionInfo.key);
                }
            }
            ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, suggest);
            final ListView autoCompleteTextView = findViewById(R.id.suggestList);
            autoCompleteTextView.setAdapter(adapter);
            // listview监听
            autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    autoCompleteTextView.setAdapter(null);
                    LatLng search_location;
                    if (mLocation == null)
                        search_location = new LatLng(41.6577396168, 123.4343104372);
                    else
                        search_location = new LatLng(mLocation.latitude, mLocation.longitude);
                    String info = suggest.get(position);
                    mPoiSearch.searchNearby(new PoiNearbySearchOption()
                            .keyword(info)
                            .location(search_location)
                            .radius(5000));
                }
            });
            adapter.notifyDataSetChanged();
        }
    };

    // poi搜索监听
    OnGetPoiSearchResultListener poiSearchResultListener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {
            if (poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                Toast.makeText(getApplicationContext(), "未找到结果", Toast.LENGTH_LONG).show();
                mPoiSearch.searchNearby(new PoiNearbySearchOption());
                return;
            }
            if (poiResult.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(getApplicationContext(), "搜索错误" + poiResult.error, Toast.LENGTH_LONG).show();
                return;
            }
            //搜索到POI
            mBaiduMap.clear();
            StringBuffer icon_name = new StringBuffer("Icon_mark0.png");
            int tem = 1;
            for (PoiInfo one_poi : poiResult.getAllPoi()) {
                icon_name.replace(9, 9, Integer.toString(tem));
                mBaiduMap.addOverlay(
                        new MarkerOptions()
                                .position(one_poi.location)
                                .icon(BitmapDescriptorFactory.fromAsset(icon_name.toString())));
                tem++;
            }
        }

        @Override
        public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {

        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {
        }

        //废弃
        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
        }
    };
}