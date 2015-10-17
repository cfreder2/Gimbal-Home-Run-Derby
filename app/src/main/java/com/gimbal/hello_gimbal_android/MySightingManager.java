package com.gimbal.hello_gimbal_android;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

public class MySightingManager {
    private Semaphore accessor = new Semaphore(1);
    private Boolean changeOccur = false;
    private ArrayList<MySighting> sightingLog;
    private Boolean hitStarted;
    private Integer sightingCount;
    private Integer maxSize;

    public MySightingManager(Integer maxSize){
        this.maxSize = maxSize;
        sightingLog = new ArrayList<>();
    }
    public Boolean addSighting(Integer rssi, Long startTime){
        try {
            accessor.acquire();
            MySighting sighting = new MySighting(rssi, startTime);

            if(hitStarted){
                sightingCount += 1;
            }

            this.sightingLog.add(sighting);
            if (this.sightingLog.size() > this.maxSize) {
                System.out.print(String.format("removing %d", sightingLog.get(0).getStartTime()));
                this.sightingLog.remove(0);

            }
            accessor.release();
            return true;
        } catch (InterruptedException ie){
            return false;
        }
    }

    public void setHitStarted(){
        sightingCount = 0;
        hitStarted = true;
    }

    public void clearHitStarted(){
        hitStarted = false;
    }

    public Long lastSightingTime(){
        try {
            accessor.acquire();
            if (sightingLog.size() > 0) {
                Long time = sightingLog.get(sightingLog.size() - 1).getStartTime();
                accessor.release();
                return time;
            }
            accessor.release();
            return (long)0;
        } catch (InterruptedException ie){
            return (long)0;
        }
    }

    public Integer avgRssi(){
        try {
            accessor.acquire();
            if(sightingLog.size()==0){
                accessor.release();
                return 0;
            }
            Iterator<MySighting> sightingIterator = this.sightingLog.iterator();
            Integer total = 0;
            while (sightingIterator.hasNext()) {
                MySighting sighting = sightingIterator.next();
                total += sighting.getRssi();
            }
            accessor.release();
            return total / sightingLog.size();
        } catch (InterruptedException ie) {
            return 0;
        }
    }

    public Long avgSightingTimeDiff(){
        return (sightingTimeDiffWithIndeces(2,1)+sightingTimeDiffWithIndeces(1,0))/2;
    }

    public Boolean didChange(){
        return changeOccur;
    }

    private Long sightingTimeDiffWithIndeces(Integer from,Integer to){
        try {
            accessor.acquire();
            if(sightingLog.size()>this.maxSize){
                accessor.release();
                return sightingLog.get(to).getStartTime() - sightingLog.get(from).getStartTime();
            }
            accessor.release();
        } catch (InterruptedException ie){
        }
        return new Long(-1);
    }

    public Boolean isReadingCompleted() {
        try {
            accessor.acquire();
            if(sightingCount>maxSize*2){
                accessor.release();
                return true;
            }
            accessor.release();
            return false;
        }
        catch (InterruptedException ie){
            return false;
        }
    }
}