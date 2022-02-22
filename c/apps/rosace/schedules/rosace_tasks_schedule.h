#pragma once

/*
 * This file was generated using SimpleSMTScheduler (https://github.com/egk696/SimpleSMTScheduler)
 * Generated schedule based on task set defined in examples/rosace_cyclic_tasks.csv
 * Scheduled Task Set Utilization = 84.22575 %
 */

#define NUM_OF_TASKS 16
#define HYPER_PERIOD 400000

#define MAPPED_CORE_COUNT 1

#define ENGINE_ID 0
#define ENGINE_PERIOD 20000
#define ELEVATOR_ID 1
#define ELEVATOR_PERIOD 20000
#define AIRCRAFT_DYN_ID 2
#define AIRCRAFT_DYN_PERIOD 20000
#define LOGGING_ID 3
#define LOGGING_PERIOD 20000
#define H_FILTER_ID 4
#define H_FILTER_PERIOD 40000
#define AZ_FILTER_ID 5
#define AZ_FILTER_PERIOD 40000
#define VZ_FILTER_ID 6
#define VZ_FILTER_PERIOD 40000
#define Q_FILTER_ID 7
#define Q_FILTER_PERIOD 40000
#define VA_FILTER_ID 8
#define VA_FILTER_PERIOD 40000
#define ALTI_HOLD_ID 9
#define ALTI_HOLD_PERIOD 80000
#define VZ_CONTROL_ID 10
#define VZ_CONTROL_PERIOD 80000
#define VA_CONTROL_ID 11
#define VA_CONTROL_PERIOD 80000
#define DELTA_E_C0_ID 12
#define DELTA_E_C0_PERIOD 80000
#define DELTA_TH_C0_ID 13
#define DELTA_TH_C0_PERIOD 80000
#define H_C0_ID 14
#define H_C0_PERIOD 400000
#define VA_C0_ID 15
#define VA_C0_PERIOD 400000

char* tasks_names[NUM_OF_TASKS] = {"ENGINE", "ELEVATOR", "AIRCRAFT_DYN", "LOGGING", "H_FILTER", "AZ_FILTER", "VZ_FILTER", "Q_FILTER", "VA_FILTER", "ALTI_HOLD", "VZ_CONTROL", "VA_CONTROL", "DELTA_E_C0", "DELTA_TH_C0", "H_C0", "VA_C0"};

unsigned tasks_per_cores[MAPPED_CORE_COUNT] = {16};

unsigned cores_hyperperiods[MAPPED_CORE_COUNT] = {400000};

unsigned tasks_coreids[NUM_OF_TASKS] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

unsigned long long tasks_periods[NUM_OF_TASKS] = {ENGINE_PERIOD, ELEVATOR_PERIOD, AIRCRAFT_DYN_PERIOD, LOGGING_PERIOD, H_FILTER_PERIOD, AZ_FILTER_PERIOD, VZ_FILTER_PERIOD, Q_FILTER_PERIOD, VA_FILTER_PERIOD, ALTI_HOLD_PERIOD, VZ_CONTROL_PERIOD, VA_CONTROL_PERIOD, DELTA_E_C0_PERIOD, DELTA_TH_C0_PERIOD, H_C0_PERIOD, VA_C0_PERIOD};

#define ENGINE_INSTS_NUM 20
#define ELEVATOR_INSTS_NUM 20
#define AIRCRAFT_DYN_INSTS_NUM 20
#define LOGGING_INSTS_NUM 20
#define H_FILTER_INSTS_NUM 10
#define AZ_FILTER_INSTS_NUM 10
#define VZ_FILTER_INSTS_NUM 10
#define Q_FILTER_INSTS_NUM 10
#define VA_FILTER_INSTS_NUM 10
#define ALTI_HOLD_INSTS_NUM 5
#define VZ_CONTROL_INSTS_NUM 5
#define VA_CONTROL_INSTS_NUM 5
#define DELTA_E_C0_INSTS_NUM 5
#define DELTA_TH_C0_INSTS_NUM 5
#define H_C0_INSTS_NUM 1
#define VA_C0_INSTS_NUM 1

unsigned tasks_insts_counts[NUM_OF_TASKS] = {ENGINE_INSTS_NUM, ELEVATOR_INSTS_NUM, AIRCRAFT_DYN_INSTS_NUM, LOGGING_INSTS_NUM, H_FILTER_INSTS_NUM, AZ_FILTER_INSTS_NUM, VZ_FILTER_INSTS_NUM, Q_FILTER_INSTS_NUM, VA_FILTER_INSTS_NUM, ALTI_HOLD_INSTS_NUM, VZ_CONTROL_INSTS_NUM, VA_CONTROL_INSTS_NUM, DELTA_E_C0_INSTS_NUM, DELTA_TH_C0_INSTS_NUM, H_C0_INSTS_NUM, VA_C0_INSTS_NUM};

unsigned long long ENGINE_sched_insts[ENGINE_INSTS_NUM] = {0, 20000, 40000, 60000, 80000, 100000, 120000, 140000, 160000, 180000, 200000, 220000, 240000, 260000, 280000, 300000, 320000, 340000, 360000, 380000};
unsigned long long ELEVATOR_sched_insts[ELEVATOR_INSTS_NUM] = {363, 20363, 40363, 60363, 80363, 100363, 120363, 140363, 160363, 180363, 200363, 220363, 240363, 260363, 280363, 300363, 320363, 340363, 360363, 380363};
unsigned long long AIRCRAFT_DYN_sched_insts[AIRCRAFT_DYN_INSTS_NUM] = {1205, 21205, 41205, 61205, 81205, 101205, 121205, 141205, 161205, 181205, 201205, 221205, 241205, 261205, 281205, 301205, 321205, 341205, 361205, 381205};
unsigned long long LOGGING_sched_insts[LOGGING_INSTS_NUM] = {17289, 37289, 57289, 77289, 97289, 117289, 137289, 157289, 177289, 197289, 217289, 237289, 257289, 277289, 297289, 317289, 337289, 357289, 377289, 397289};
unsigned long long H_FILTER_sched_insts[H_FILTER_INSTS_NUM] = {14619, 54619, 94619, 134619, 174619, 214619, 254619, 294619, 334619, 374619};
unsigned long long AZ_FILTER_sched_insts[AZ_FILTER_INSTS_NUM] = {15008, 55008, 95008, 135008, 175008, 215008, 255008, 295008, 335008, 375008};
unsigned long long VZ_FILTER_sched_insts[VZ_FILTER_INSTS_NUM] = {15397, 55397, 95397, 135397, 175397, 215397, 255397, 295397, 335397, 375397};
unsigned long long Q_FILTER_sched_insts[Q_FILTER_INSTS_NUM] = {34405, 74405, 114405, 154405, 194405, 234405, 274405, 314405, 354405, 394405};
unsigned long long VA_FILTER_sched_insts[VA_FILTER_INSTS_NUM] = {34799, 74799, 114799, 154799, 194799, 234799, 274799, 314799, 354799, 394799};
unsigned long long ALTI_HOLD_sched_insts[ALTI_HOLD_INSTS_NUM] = {75188, 155188, 235188, 315188, 395188};
unsigned long long VZ_CONTROL_sched_insts[VZ_CONTROL_INSTS_NUM] = {75546, 155546, 235546, 315546, 395546};
unsigned long long VA_CONTROL_sched_insts[VA_CONTROL_INSTS_NUM] = {76179, 156179, 236179, 316179, 396179};
unsigned long long DELTA_E_C0_sched_insts[DELTA_E_C0_INSTS_NUM] = {76885, 156885, 236885, 316885, 396885};
unsigned long long DELTA_TH_C0_sched_insts[DELTA_TH_C0_INSTS_NUM] = {77087, 157087, 237087, 317087, 397087};
unsigned long long H_C0_sched_insts[H_C0_INSTS_NUM] = {14405};
unsigned long long VA_C0_sched_insts[VA_C0_INSTS_NUM] = {991};

unsigned long long *tasks_schedules[NUM_OF_TASKS] = {ENGINE_sched_insts, ELEVATOR_sched_insts, AIRCRAFT_DYN_sched_insts, LOGGING_sched_insts, H_FILTER_sched_insts, AZ_FILTER_sched_insts, VZ_FILTER_sched_insts, Q_FILTER_sched_insts, VA_FILTER_sched_insts, ALTI_HOLD_sched_insts, VZ_CONTROL_sched_insts, VA_CONTROL_sched_insts, DELTA_E_C0_sched_insts, DELTA_TH_C0_sched_insts, H_C0_sched_insts, VA_C0_sched_insts};
