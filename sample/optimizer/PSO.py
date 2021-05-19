# -*- coding: utf-8 -*-
"""
@cite: 
% _____________________________________________________
% title = {Black-Box Optimization Benchmarking for Noiseless Function Testbed Using Particle Swarm Optimization},
% author = {El-Abd, Mohammed and Kamel, Mohamed S.},
% booktitle = {Proceedings of the 11th Annual Conference Companion on Genetic and Evolutionary Computation Conference: Late Breaking Papers},
% series = {GECCO '09},
% numpages = {6},
% pages = {2269--2274},
% year = {2009},
% publisher = {Association for Computing Machinery},
%  location = {Montreal QC, Canada},
% address = {New York, NY, USA}
% _____________________________________________________

"""

import csv
import random
import sys
import time
import numpy as np
import math
import os
import numpy.matlib
from multiprocessing import Pool
from multiprocessing import Process

START_CYCLE = 0
FINISH_CYCLE = 1
POP_SIZE = 40
NAIVE = 1
RATIONAL = 1
RUBY = 1
PEOPLE_SIZE_RATE = 0.01
POOL_SIZE = 20
FILEPATH = ""

SIGMA_RANGE = [3.0, 100.0]
MU_RANGE = [0.0, 300.0]
PI_RANGE = [0.0, 1.0]

if len(sys.argv) == 8:
    START_CYCLE = int(sys.argv[1])
    FINISH_CYCLE = int(sys.argv[2])
    POP_SIZE = int(sys.argv[3])
    NAIVE = int(sys.argv[4])
    RATIONAL = int(sys.argv[5])
    RUBY = int(sys.argv[6])
    FILEPATH = sys.argv[7]
else :
    sys.exit(1)

#python PSO.py 0 50 40 1 1 1

#HP
c1 = 1.4944
c2 = 1.4944
w = 0.792
vxmax = 1.0
vxmin = 0.0
DIM = (NAIVE + RATIONAL + RUBY) * 3 - 1

def function(arx,N,lam):
    fitness = np.zeros([lam])
    for i in range(lam):
        fitness[i] = sum((arx[i,:] - 0.1)*(arx[i,:] - 0.1))

    #print(fitness)
    return fitness

def csv_append(csv_columns,colmuns_name,agent_size):
    for i in range(agent_size):
        csv_columns.append("sigma_"+colmuns_name+str(i))
        csv_columns.append("mu_"+colmuns_name+str(i))
        csv_columns.append("pi_"+colmuns_name+str(i))

def people_size_check(mu, pB, pC, pP, t):
    result = 0.0
    pA = (t - mu)**2.0
    result = sum(pP*(np.exp(-1.0 * pA / pB) / pC))
    return result

def norm_value(sigma, mu, pP):
    sum_people = 0.0
    max_people = 0.0
    pB = (sigma**2.0)*2.0
    pC = np.sqrt((sigma**2.0)*math.pi*2.0)
    for t in range(300):
        res = people_size_check(mu, pB, pC, pP, t)
        sum_people += res
        max_people = max(max_people, res)
    return max_people * 1.0 / sum_people

def PSO(CURRENT_CYCLE):
    #next cycle
    Mixture = NAIVE + RATIONAL + RUBY
    csv_columns = []
    csv_append(csv_columns,"naive",NAIVE)
    csv_append(csv_columns,"rational",RATIONAL)
    csv_append(csv_columns,"ruby",RUBY)

    # Set algorithm parameters
    x = np.zeros((POP_SIZE,DIM))
    v = np.zeros((POP_SIZE,DIM))
    pbest = np.zeros((POP_SIZE,DIM))
    cost_p = np.zeros(POP_SIZE)
    cost_x = np.zeros(POP_SIZE)
    if CURRENT_CYCLE > 0:
        with open(FILEPATH+'previous_parameter/MatrixList_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(1,POP_SIZE*DIM + 1):
                x[int((y - 1) / DIM)][int((y - 1) % DIM)] = float(num[y][0])
                v[int((y - 1) / DIM)][int((y - 1) % DIM)] = float(num[y][1])
                pbest[int((y - 1) / DIM)][int((y - 1) % DIM)] = float(num[y][2])
        
        with open(FILEPATH+'previous_parameter/ArrayList_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(1,POP_SIZE+1):
                cost_p[y - 1] = float(num[y][0])
        
        # Update pbest and gbest if necessary
        with open(FILEPATH+'previous_parameter/FitnessList.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(1,POP_SIZE+1):
                cost_x[y - 1] = float(num[y][0])

        for p in range(POP_SIZE):
            if cost_x[p] < cost_p[p]:
                cost_p[p] = cost_x[p]
                pbest[p,:] = x[p,:]

        # update pbest and gbest
        cost_g = np.min(cost_p)
        gbest = pbest[np.argmin(cost_p),:]

    # Update inertia weight    
    w = 0.09 - 0.07*(CURRENT_CYCLE/FINISH_CYCLE);    
    current_pop = 0
    
    LoopC = 0
    while current_pop < POP_SIZE:
        LoopC += 1
        #Update velocity and position
        rr1 = np.random.rand(DIM)
        rr2 = np.random.rand(DIM)
        xx = vxmax * np.random.rand(DIM)
        vv = vxmax * np.random.rand(DIM)
        if CURRENT_CYCLE == 1:
            vv2 = vxmax * np.random.rand(DIM)
            vv = w*vv2 + c1 * rr1*(pbest[current_pop,:] - x[current_pop,:]) + c2 * rr2 * (gbest - x[current_pop,:])
            xx = x[current_pop,:] + vv
            xx = np.clip(xx,vxmin,vxmax)
        elif CURRENT_CYCLE > 1 and LoopC < 10000:
            vv = w*v[current_pop,:] + c1 * rr1*(pbest[current_pop,:] - x[current_pop,:]) + c2 * rr2 * (gbest - x[current_pop,:])
            xx = x[current_pop,:] + vv
            xx = np.clip(xx,vxmin,vxmax)

        sum_pi = 0.0
        
        # set value pi
        pi = xx[2::3]
        pi = np.append(pi,1.0 - sum(pi))
        # sum_pi = 1.0
        if(pi[Mixture - 1] < 0.0):
            continue

        # set value other norm
        sigma = xx[0::3] * (SIGMA_RANGE[1] - SIGMA_RANGE[0]) + SIGMA_RANGE[0]
        mu = xx[1::3] * (MU_RANGE[1] - MU_RANGE[0]) + MU_RANGE[0]
            
        # check people size for one sepalate
        rate = norm_value(sigma, mu, pi)
        if rate <= PEOPLE_SIZE_RATE:
            LoopC = 0
            csv_datasets = []
            for j in range(Mixture):
                csv_datasets.append(sigma[j])
                csv_datasets.append(mu[j])
                csv_datasets.append(pi[j])
            with open(FILEPATH+'population_'+str(current_pop)+'.csv', 'w') as f:
                writer = csv.writer(f, lineterminator='\n')
                writer.writerow(csv_columns)
                writer.writerow(csv_datasets)
            v[current_pop,:] = vv
            x[current_pop,:] = xx
            current_pop = current_pop + 1
    
    if CURRENT_CYCLE == 0:
        pbest = x
        cost_p = np.ones(POP_SIZE)*1000000
        # update pbest and gbest   
        cost_g = np.min(cost_p)
        gbest = pbest[np.argmin(cost_p),:]
    
    with open(FILEPATH+'current_parameter/MatrixList_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(["x","v","pbest"])
        for p in range(POP_SIZE):
            for d in range(DIM):
                writer.writerow([x[p][d],v[p][d],pbest[p][d]])

    with open(FILEPATH+'current_parameter/ArrayList_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(["cost_p"])
        for y in range(POP_SIZE):
            writer.writerow([cost_p[y]])

    #crowdwalk
    #cost_x = np.ones(POP_SIZE)*1000000
    #with open(FILEPATH+'current_parameter/FitnessList.csv', 'w') as f:
    #    writer = csv.writer(f, lineterminator='\n')
    #    writer.writerow(["FitnessList"])
    #    for y in range(POP_SIZE):
    #        writer.writerow([cost_x[y]])
    print(CURRENT_CYCLE,cost_g)

if __name__ == "__main__":
    for i in range(START_CYCLE,FINISH_CYCLE):
        PSO(i)
