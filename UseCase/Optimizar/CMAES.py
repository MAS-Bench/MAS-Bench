# -*- coding: utf-8 -*-
"""
@author: 
% _____________________________________________________
%   author = "Nikolaus Hansen and Andreas Ostermeier",
%  title = "Completely Derandomized Self-Adaptation in Evolution Strategies",
%  journal = "Evol. Comput.",
%  volume = "9",
%  number = "2",
%  pages = "159–-195",
%  year = "2001",
%  month = "Jun"
% _____________________________________________________

"""

import csv
import random
import sys
import time
import numpy as np
import math
import os
from multiprocessing import Pool
from multiprocessing import Process
import pandas as pd

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

#python CMAES.py 0 50 40 1 1 1

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

#HP
START_SIGMA = 0.2
RANK_MU = int(POP_SIZE / 2)

def function(arx,N,lam):
    fitness = np.zeros([lam])
    for i in range(lam):
        fitness[i] = sum((arx[:,i] - 0.1)*(arx[:,i] - 0.1))

    #print(fitness)
    return fitness

def csv_append(csv_columns,colmuns_name,agent_size):
    for i in range(agent_size):
        csv_columns.append("sigma_"+colmuns_name+str(i))
        csv_columns.append("mu_"+colmuns_name+str(i))
        csv_columns.append("pi_"+colmuns_name+str(i))

def people_size_check(current_pop, mu, pB, pC, pP, dimension, t):
    cp = current_pop
    result = 0.0
    pA = (t - mu[cp])**2.0
    result = sum(pP*(np.exp(-1.0 * pA / pB) / pC))
    return result

def norm_value(cp, sigma, mu, pi_G, pi_G_F, dimension):
    sum_people = 0.0
    max_people = 0.0
    pB = (sigma[cp]**2.0)*2.0
    pC = np.sqrt((sigma[cp]**2.0)*math.pi*2.0)
    pP = np.append(pi_G[cp],pi_G_F[cp])
    for t in range(300):
        res = people_size_check(cp, mu, pB, pC, pP, dimension, t)
        sum_people += res
        max_people = max(max_people, res)
    return max_people * 1.0 / sum_people

def CMAES(CURRENT_CYCLE):
    #dimension
    N = (NAIVE + RATIONAL + RUBY) * 3 - 1
    xmean=None
    sigma=START_SIGMA
    arfitness = np.zeros([POP_SIZE])
    startDistMu = np.zeros([N])

    xmean = np.random.randn(N,1) if xmean is None else xmean
    xmean = np.zeros((N,1)) + 0.5
    lam = POP_SIZE
    mu = RANK_MU
    weights = np.log((lam + 1) / 2) - np.log(np.arange(mu) + 1)
    weights /= weights.sum()
    mueff = (weights.sum() ** 2) / (weights ** 2).sum()
    
    #normal_CMAES
    cc = (4 + mueff / N) / (N + 4 + 2 * mueff / N)
    cs = (mueff + 2) / (N + mueff + 5)
    alpha_cov = 2
    c1 = alpha_cov / ((N + 1.3) ** 2 + mueff)
    cmu = min(1 - c1, alpha_cov * (mueff - 2 + 1 / mueff) / ((N + 2) ** 2 + alpha_cov * mueff / 2))
    damps = 1 + 2 * max(0, np.sqrt((mueff - 1) / (N + 1)) - 1) + cs
    pc = np.zeros([N, 1])
    ps = np.zeros([N, 1])
    B = np.eye(N)
    D = np.ones([N, 1])
    C = B @ np.diag((D ** 2).flatten()) @ B.T
    invsqrtC = B @ np.diag((D ** -1).flatten()) @ B.T
    eigenval = 0
    chiN = N ** 0.5 * (1 - 1 / (4 * N) + 1 / (21 + N ** 2))
    counteval = 0
    NP = np.random.randn(N, lam)
    arx = xmean + sigma * (B @ (D * NP))
    arfitness = function(arx,N,lam)
    #arfitness = numpy.ndarray [1.0,2.0,3.0]

    #prev cycle value
    if CURRENT_CYCLE > 0:
        with open(FILEPATH+'previous_parameter/FitnessList.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(1,lam+1):
                arfitness[y - 1] = float(num[y][0])

        with open(FILEPATH+'previous_parameter/ValueList_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            eigenval = int(num[1][0])
            counteval = int(num[1][1])
            sigma = float(num[1][2])

        with open(FILEPATH+'previous_parameter/ArrayList_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(1,N+1):
                xmean[y-1][0] = float(num[y][0])
                pc[y-1][0] = float(num[y][1])
                ps[y-1][0] = float(num[y][2])
                D[y-1][0] = float(num[y][3])

        with open(FILEPATH+'previous_parameter/MatrixList_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(1,N*N + 1):
                B[int((y - 1) / N)][int((y - 1) % N)] = float(num[y][0])
                C[int((y - 1) / N)][int((y - 1) % N)] = float(num[y][1])
                invsqrtC[int((y - 1) / N)][int((y - 1) % N)] = float(num[y][2])

        with open(FILEPATH+'previous_parameter/PopulationList_'+str(CURRENT_CYCLE - 1)+'.csv') as f:
            reader = csv.reader(f)
            num = [row for row in reader]
            for y in range(N):
                for x in range(lam):
                    arx[y][x] = float(num[y][x])

        #get DA Fitness
        #arfitness = function(arx,N,lam)
        counteval += lam

        # Sort by fitness and compute weighted mean into xmean
        arindex = np.argsort(arfitness)
        arfitness = arfitness[arindex]
        xold = xmean
        xmean = (arx[:, arindex[:mu]] @ weights).reshape([-1, 1])
        
        # Cumulation: Update evolution paths
        ps = (1 - cs) * ps + np.sqrt(cs * (2 - cs) * mueff) * invsqrtC @ (xmean - xold) / sigma
        hsig = (ps ** 2).sum() / (1 - (1 - cs) ** (2 * counteval / lam)) / N < 2 + 4 / (N + 1)
        pc = (1 - cc) * pc + hsig * np.sqrt(cc * (2 - cc) * mueff) * (xmean - xold) / sigma

        # Adapt covariance matrix C
        artmp = (1 / sigma) * (arx[:, arindex[:mu]] - xold)
        C = (1 - c1 - cmu) * C + \
            c1 * (pc @ pc.T + (1 - hsig) * cc * (2 - cc) * C) + \
            cmu * artmp @ np.diag(weights) @ artmp.T

        # Adapt step size sigma
        sigma *= np.exp((cs / damps) * (np.linalg.norm(ps) / chiN - 1))

        # Update B and D from C
        if counteval - eigenval > lam / (c1 + cmu) / N / 10:
            eigenval = counteval
            # C = np.triu(C) + np.triu(C, 1).T
            D, B = np.linalg.eigh(C, 'U')
            D = np.sqrt(D.real).reshape([N, 1])
            invsqrtC = B @ np.diag((D ** -1).flatten()) @ B.T

        if np.allclose(arfitness[0], arfitness[int(1 + 0.7 * lam)], 1e-10, 1e-10):
            sigma *= np.exp(0.2 + cs / damps)

        print("%d,%d,%g" % (CURRENT_CYCLE,CURRENT_CYCLE * lam, arfitness[0]))

        #NP = np.random.randn(N, lam)
        NP = np.random.randn(N, lam)
        arx = xmean + sigma * (B @ (D * NP))
    
    #next cycle
    Mixture = NAIVE + RATIONAL + RUBY
    csv_columns = []
    csv_append(csv_columns,"naive",NAIVE)
    csv_append(csv_columns,"rational",RATIONAL)
    csv_append(csv_columns,"ruby",RUBY)
    sigma_GMM = np.zeros((POP_SIZE, Mixture))
    mu_GMM = np.zeros((POP_SIZE, Mixture))
    pi_GMM = np.zeros((POP_SIZE, Mixture - 1))
    pi_GMM_F = np.zeros((POP_SIZE, 1))
    start = time.time()
    
    current_pop = 0
    sBDsub = sigma * (B @ D)

    cntA = cntB = cntC = 0
    xmean_min = -xmean
    xmean_max = 1.0 - xmean
    xmean_sum = 1.0 - np.sum(xmean[2::3].flatten())
    while current_pop < POP_SIZE:
        sum_pi = 0.0
        Nsub = np.random.randn(N,1)
        arxsub = sBDsub * Nsub
        # minus velue check
        if(np.any((arxsub < xmean_min) | (arxsub > xmean_max))):
            cntA += 1
            continue
        if np.sum(arxsub[2::3].flatten()) > xmean_sum:
            cntB += 1
            continue
        
        arxsub = xmean + arxsub
        # set value pi
        pi_GMM[current_pop,:] = arxsub[2::3].flatten()        
        pi_sum = np.sum(pi_GMM[current_pop,:])
        pi_GMM_F[current_pop][0] = 1.0 - pi_sum 
        
        # set value other norm
        sigma_GMM[current_pop,:] = arxsub[0::3].flatten()*SIGMA_RANGE[1]
        mu_GMM[current_pop,:] = arxsub[1::3].flatten()*MU_RANGE[1]

        # check people size for one sepalate
        rate = norm_value(current_pop, sigma_GMM, mu_GMM, pi_GMM,pi_GMM_F, Mixture)
        cntC += 1
        if rate <= PEOPLE_SIZE_RATE:
            #print ("current_pop:"+str(current_pop)+" cntA:"+str(cntA)+" cntB:"+str(cntB)+" cntC:"+str(cntC)+" elapsed_time:{0}".format(time.time() - start) + "[sec]")
            csv_datasets = []
            for j in range(Mixture):
                csv_datasets.append(sigma_GMM[current_pop][j])
                csv_datasets.append(mu_GMM[current_pop][j])
                if j < Mixture - 1:
                    csv_datasets.append(pi_GMM[current_pop][j])
                else:
                    csv_datasets.append(pi_GMM_F[current_pop][0])
            df = pd.DataFrame([csv_datasets], columns=csv_columns)
            df.to_csv(FILEPATH+'population_'+str(current_pop)+'.csv',index=False)
            arx[:,current_pop] = arxsub[:,0]
            current_pop = current_pop + 1
            cntA = cntB = cntC = 0
    
    with open(FILEPATH+'current_parameter/PopulationList_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
        writer = csv.writer(f, lineterminator='\n')
        for y in range(N):
            csvlist2 = []
            for x in range(lam):
                csvlist2.append(arx[y][x])

            writer.writerow(csvlist2)

    with open(FILEPATH+'current_parameter/ValueList_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(["eigenval","counteval","sigma"])
        writer.writerow([eigenval,counteval,sigma])

    with open(FILEPATH+'current_parameter/ArrayList_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(["xmean","pc","ps","D"])
        for y in range(N):
            writer.writerow([xmean[y][0],pc[y][0],ps[y][0],D[y][0]])

    with open(FILEPATH+'current_parameter/MatrixList_'+str(CURRENT_CYCLE)+'.csv', 'w') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(["B","C","invsqrtC"])
        for y in range(N):
            for x in range(N):
                writer.writerow([B[y][x],C[y][x],invsqrtC[y][x]])

    #crowdwalk
    #with open(FILEPATH+'current_parameter/FitnessList.csv', 'w') as f:
    #    writer = csv.writer(f, lineterminator='\n')
    #    writer.writerow(["FitnessList"])
    #    for y in range(lam):
    #        writer.writerow([arfitness[y]])

if __name__ == "__main__":
    for i in range(START_CYCLE,FINISH_CYCLE):
        CMAES(i)
