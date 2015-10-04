package com.gimbal.hello_gimbal_android;

import java.util.ArrayList;
import java.util.Iterator;
//s
public class MySightingManager {
    private ArrayList<MySighting> sightingLog;
    private Integer maxSize;

    public MySightingManager(Integer maxSize){
        this.maxSize = maxSize;
        sightingLog = new ArrayList<>();
    }
    public void addSighting(Integer rssi, Long startTime){
        MySighting sighting = new MySighting(rssi,startTime);

        this.sightingLog.add(sighting);
        if(this.sightingLog.size()>this.maxSize){
            this.sightingLog.remove(0);
        }
    }

    public MySighting mostRecentSighting(){
        if(sightingLog.size()>0){
            return this.sightingLog.get(sightingLog.size()-1);
        }
        return new MySighting();
    }

    public Integer avgRssi(){
        Iterator<MySighting> sightingIterator = this.sightingLog.iterator();
        Integer total = 0;
        while(sightingIterator.hasNext()){
            MySighting sighting = sightingIterator.next();
            total += sighting.getRssi();
        }
        return total/sightingLog.size();
    }

    public Long avgSightingTimeDiff(){
        return (sightingTimeDiffWithIndeces(2,1)+sightingTimeDiffWithIndeces(1,0))/2;
    }

    private Long sightingTimeDiffWithIndeces(Integer from,Integer to){
        if(sightingLog.size()>this.maxSize){
            return sightingLog.get(to).getStartTime() - sightingLog.get(from).getStartTime();
        }
        return new Long(-1);
    }
}