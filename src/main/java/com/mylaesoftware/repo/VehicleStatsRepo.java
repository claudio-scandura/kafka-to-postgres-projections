package com.mylaesoftware.repo;

import akka.japi.Pair;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface VehicleStatsRepo {

    CompletionStage<List<Pair<String, Integer>>> findFastestVechicles(int limit);
}
