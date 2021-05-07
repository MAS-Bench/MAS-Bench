# -*- coding: utf-8 -*-
"""
@cite: 
% _____________________________________________________
%  title={Success-history based parameter adaptation for differential evolution},
%  author={Tanabe, Ryoji and Fukunaga, Alex},
%  booktitle={2013 IEEE congress on evolutionary computation},
%  pages={71--78},
%  year={2013},
%  location = {Cancun, Mexico}
%  organization={IEEE}
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
from scipy import stats

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

DIM = (NAIVE + RATIONAL + RUBY) * 3 - 1
Mixture = NAIVE + RATIONAL + RUBY

def function(arx,N,lam):
    fitness = np.zeros([lam])
    for i in range(lam):
        fitness[i] = sum((arx[i,:] - 0.1)*(arx[i,:] - 0.1))
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

def generatecr(mcr,r):
    cr = stats.norm.rvs(loc=mcr[r], scale=0.1)
    # clipping to [0, 1]
    return np.clip(cr, 0., 1.)

def generatef(mf,r):
    f = -1
    while f <= 0.:
        f = stats.cauchy.rvs(loc=mf[r], scale=0.1)
        # if Fi >= 1., then set as 1.
        # if Fi <= 0., then regenerate Fi.
        if f >= 1.:
            f = 1.
    return f

def getxbestp(xcurrent,fcurrent,isminimize ,p):
    # top 100p %
    topn = max(int(float(POP_SIZE) * p), 2)
    topidx = np.argsort(fcurrent)[:topn] if isminimize else np.argsort(fcurrent)[::-1][:topn]
    bestpidx = np.random.choice(topidx)
    return xcurrent[bestpidx]

def selection(xcurrent,lowlim,uplim,fcurrent,isminimize, p, u, fu):
    # score is better than current
    q1 = fu <= fcurrent[p] if isminimize else fu >= fcurrent[p]
    # over lower limit
    q2 = np.any(u < lowlim)
    # over upper limit
    q3 = np.any(u > uplim)
    # q1 ^ ~q2 ^ ~q3
    q = q1 * ~q2 * ~q3
    
    fp1 = fu if q else fcurrent[p]
    xp1 = u if q else xcurrent[p]
    
    return p, fp1, xp1, q

def mutation(archive,xcurrent,fcurrent,isminimize, current, sf):
    # x p-best
    xpbest = getxbestp(xcurrent,fcurrent,isminimize ,current)
    # x r1
    r1 = np.random.choice([n for n in range(POP_SIZE) if n != current])
    xr1 = xcurrent[r1]
    # x~ r2
    # randomly selection from population ^ archive
    r2 = np.random.choice([n for n in range(POP_SIZE) if n not in [r1, current]]
                           + list(range(POP_SIZE, POP_SIZE + len(archive))))
    if len(archive):
        xr2 = np.concatenate([xcurrent, np.r_[archive]])[r2]
    else:
        xr2 = xcurrent[r2]
    # v
    v = xcurrent[current] + sf * (xpbest - xcurrent[current]) + sf * (xr1 - xr2)
    return v

def crossover(v, x, cr):
    # crossover
    r = np.random.choice(range(DIM))
    u = np.zeros(DIM)
    # binary crossover
    flg = np.equal(r, np.arange(DIM)) + np.random.rand(DIM) < cr
    # from mutant vector
    u[flg] = v[flg]
    # from current vector
    u[~flg] = x[~flg]
    return u

def lehmermean(s):
    return np.sum(np.array(s) ** 2) / np.sum(s)

def mutationcrossover(h, archive,k,mcr,mf,xcurrent,lowlim,uplim,fcurrent,isminimize):
    lup = []
    lsf = []
    lcr = []
    csv_columns = []
    csv_append(csv_columns,"naive",NAIVE)
    csv_append(csv_columns,"rational",RATIONAL)
    csv_append(csv_columns,"ruby",RUBY)

    # for each individuals
    p = 0
    while p < POP_SIZE:
        # generate F and Cr
        ri = np.random.choice(range(h))
        sf = generatef(mf,ri)
        cr = generatecr(mcr,ri)
        # mutation
        vp = mutation(archive,xcurrent,fcurrent,isminimize, p, sf=sf)
        vp = np.clip(vp,0,1)
        # crossover
        up = crossover(vp, xcurrent[p], cr=cr)
        # set value pi
        pi = up[2::3]
        pi = np.append(pi,1.0 - sum(pi))
        # sum_pi = 1.0
        if(pi[Mixture - 1] < 0.0):
            continue
        
        # set value other norm
        sigma = up[0::3] * (SIGMA_RANGE[1] - SIGMA_RANGE[0]) + SIGMA_RANGE[0]
        mu = up[1::3] * (MU_RANGE[1] - MU_RANGE[0]) + MU_RANGE[0]

        # check people size for one sepalate
        rate = norm_value(sigma, mu, pi)
        if rate <= PEOPLE_SIZE_RATE:
            csv_datasets = []
            for j in range(Mixture):
                csv_datasets.append(sigma[j])
                csv_datasets.append(mu[j])
                csv_datasets.append(pi[j])
            with open(FILEPATH+'population_'+str(p)+'.csv', 'w') as f:
                writer = csv.writer(f, lineterminator='\n')
                writer.writerow(csv_columns)
                writer.writerow(csv_datasets)
            
            # storing trial vectors, scaling-factor, crossover-rate
            lup.append(up)
            lsf.append(sf)
            lcr.append(cr)
            p = p + 1
    return lup, lsf, lcr

def SHADE(CURRENT_CYCLE):
    isminimize = True
    p = 0.1
    arc_rate = 2;
    h = Mixture*40
    lowlim = np.zeros(DIM)
    uplim = np.ones(DIM)
    xcurrent = np.random.rand(POP_SIZE, DIM) * (uplim - lowlim) + lowlim
    lupsub = xcurrent
    archive = []
    mcr = [0.5 for _ in range(h)]
    mf = [0.5 for _ in range(h)]
    # initialize scr, sf
    scr = []
    sf = []
    lup = []
    
    if CURRENT_CYCLE == 1:
        fcurrent = np.zeros(POP_SIZE)
        with open(FILEPATH+'previous_parameter/FitnessList.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(1,POP_SIZE+1):
                fcurrent[y - 1] = float(num[y][0])

        with open(FILEPATH+'previous_parameter/xcurrent_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(POP_SIZE):
                for x in range(DIM):
                    xcurrent[y][x] = float(num[y][x])
    
    if CURRENT_CYCLE > 1:
        fu = np.zeros(POP_SIZE)
        fcurrent = np.zeros(POP_SIZE)
        lsf = np.zeros(POP_SIZE)
        lcr = np.zeros(POP_SIZE)
        with open(FILEPATH+'previous_parameter/FitnessList_F_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(1,POP_SIZE+1):
                fcurrent[y - 1] = float(num[y][0])

        with open(FILEPATH+'previous_parameter/FitnessList.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(1,POP_SIZE+1):
                fu[y - 1] = float(num[y][0])
        
        #POP_Value
        with open(FILEPATH+'previous_parameter/xcurrent_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(POP_SIZE):
                for x in range(DIM):
                    xcurrent[y][x] = float(num[y][x])
                
        with open(FILEPATH+'previous_parameter/mcr-sf_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(1,(Mixture*40)+1):
                mcr[y - 1] = float(num[y][0])
                mf[y - 1] = float(num[y][1])
        
        with open(FILEPATH+'previous_parameter/lupsub_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            for row in reader:
                lup.append(np.array([float(s) for s in row]))

        
        with open(FILEPATH+'previous_parameter/lsf-lcr_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(1,POP_SIZE+1):
                lsf[y - 1] = float(num[y][0])
                lcr[y - 1] = float(num[y][1])
        
        if CURRENT_CYCLE > 2: 
            with open(FILEPATH+'previous_parameter/archive_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
                reader = csv.reader(f)
                for row in reader:
                    archive.append(np.array([float(s) for s in row]))
       
        for p, up in enumerate(lup):
            # selection
            fxq = selection(xcurrent,lowlim,uplim,fcurrent,isminimize,p, up, fu[p])
            fp1 = fxq[1]
            xp1 = fxq[2]
            q = fxq[3]
            # storing parent-x, cr, f
            if q:
                archive.append(xcurrent[p].copy())
                sf.append(lsf[p])
                scr.append(lcr[p])
            # update current values
            fcurrent[p] = fp1
            xcurrent[p] = xp1
        
        # remove an individual from archive when size of archive is larger than population.
        if len(archive) > POP_SIZE:
            r = np.random.choice(range(len(archive)), len(archive) - POP_SIZE, replace=False)
            arc = [archive[a] for a in range(len(archive)) if a not in r]
            archive = arc
        # update mf, mcr
        if len(sf) > 0 and len(scr) > 0:
            mf[CURRENT_CYCLE] = lehmermean(sf)
            mcr[CURRENT_CYCLE] = lehmermean(scr)
    
    #CrowdWalk
    if CURRENT_CYCLE == 0:
        csv_columns = []
        csv_append(csv_columns,"naive",NAIVE)
        csv_append(csv_columns,"rational",RATIONAL)
        csv_append(csv_columns,"ruby",RUBY)
    
        # for each individuals
        p = 0
        while p < POP_SIZE:
            xx = np.random.rand(DIM)
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
                LoopA = LoopB = LoopC = 0
                csv_datasets = []
                for j in range(Mixture):
                    csv_datasets.append(sigma[j])
                    csv_datasets.append(mu[j])
                    csv_datasets.append(pi[j])
                with open(FILEPATH+'population_'+str(p)+'.csv', 'w') as f:
                    writer = csv.writer(f, lineterminator='\n')
                    writer.writerow(csv_columns)
                    writer.writerow(csv_datasets)
                
                xcurrent[i,:] = xx
                # storing trial vectors, scaling-factor, crossover-rate
                p = p + 1


        fcurrent = function(xcurrent,DIM,POP_SIZE)
        #with open(FILEPATH+'current_parameter/FitnessList.csv', 'w') as f:
        #    writer = csv.writer(f, lineterminator='\n')
        #    writer.writerow(["FitnessList"])
        #    for y in range(POP_SIZE):
        #        writer.writerow([fcurrent[y]])
    else :
        # mutation and crossover
        lup, lsf, lcr = mutationcrossover(h, archive,CURRENT_CYCLE,mcr,mf,xcurrent,lowlim,uplim,fcurrent,isminimize)
        lupsub = np.array(lup)
        fu = function(lupsub, DIM, POP_SIZE)
        #with open(FILEPATH+'current_parameter/FitnessList.csv', 'w') as f:
        #    writer = csv.writer(f, lineterminator='\n')
        #    writer.writerow(["FitnessList"])
        #    for y in range(POP_SIZE):
        #        writer.writerow([fu[y]])

        with open(FILEPATH+'current_parameter/FitnessList_F_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
            writer = csv.writer(f, lineterminator='\n')
            writer.writerow(["FitnessList"])
            for y in range(POP_SIZE):
                writer.writerow([fcurrent[y]])

        with open(FILEPATH+'current_parameter/lupsub_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
            writer = csv.writer(f, lineterminator='\n')
            for y in range(POP_SIZE):
                csvlist2 = []
                for x in range(DIM):
                    csvlist2.append(lup[y][x])
                writer.writerow(csvlist2)

        with open(FILEPATH+'current_parameter/lsf-lcr_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
            writer = csv.writer(f, lineterminator='\n')
            writer.writerow(["lsf","lcr"])
            for y in range(POP_SIZE):
                writer.writerow([lsf[y],lcr[y]])
        
        if CURRENT_CYCLE > 1:
            with open(FILEPATH+'current_parameter/archive_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
                writer = csv.writer(f, lineterminator='\n')
                for y in range(len(archive)):
                    csvlist2 = []
                    for x in range(DIM):
                        csvlist2.append(archive[y][x])
                    writer.writerow(csvlist2)

    #POP_Value
    with open(FILEPATH+'current_parameter/xcurrent_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
        writer = csv.writer(f, lineterminator='\n')
        for y in range(POP_SIZE):
            csvlist2 = []
            for x in range(DIM):
                csvlist2.append(xcurrent[y][x])
            writer.writerow(csvlist2)
    
    with open(FILEPATH+'current_parameter/mcr-sf_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(["mcr","mf"])
        for y in range(Mixture*40):
            writer.writerow([mcr[y],mf[y]])
    
    #Other
    print(min(fcurrent))

if __name__ == "__main__":
    for i in range(START_CYCLE,FINISH_CYCLE):
        SHADE(i)
