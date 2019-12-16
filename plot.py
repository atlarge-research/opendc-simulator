import matplotlib.pyplot as plt
import numpy as np
import csv

# exps = [("./data/", "CPOP1"),
#         ("./heft/", "HEFT"),
#         ("./data/", "CPOP2")]


setups = ["setup", "setup-heterogeneous", "setup-distributed"]
prettySetups = ["homogeneous setup", "heterogeneous setup", "distributed setup"]
traces = ["shell", "askalon", "pegasus"]
schedulers = ["FCP", "Lottery", "DS"]

#############################################################################
##########             JOB METRICS                             ##############
#############################################################################
results = {}
# for folder, name in exps:
for setup in setups:
    results[setup] = {}
    for trace in traces:
        results[setup][trace] = {}
        for sched in schedulers:
            results[setup][trace][sched] = {}
            with open("./results/" + trace + "_" + setup + "_" + sched + "/job_metrics.csv", 'r') as csvfile:
                rd = csv.DictReader(csvfile)
                cp = []
                wt = []
                ms = []
                for row in rd:
                    cp.append(int(row["critical_path"]))
                    wt.append(int(row["waiting_time"]))
                    ms.append(int(row["makespan"]))
                results[setup][trace][sched]["CP"] = [np.mean(cp), np.std(cp), np.min(cp), np.max(cp)]
                results[setup][trace][sched]["WT"] = [np.mean(wt), np.std(wt), np.min(wt), np.max(wt)]
                results[setup][trace][sched]["MS"] = [np.mean(ms), np.std(ms), np.min(ms), np.max(ms)]


labels = ['AVG', 'MIN', 'MAX']

x = np.arange(len(labels))  # the label locations
width = 0.20  # the width of the bars

pos = [0, 0.25, 0.50, 1.0, 1.25, 1.5, 2, 2.25, 2.5]
metrics = ['CP', 'WT', 'MS']
xticks = []
for met in metrics:
    for sched in schedulers:
        xticks.append(sched)

for s, setup in enumerate(setups):
    fig, ax = plt.subplots(1, 3, figsize=(12,6))
    for t, trace in enumerate(traces):
        # ax = plt.subplot(1, 3, t + 1)
        avg = []
        std = []
        mn = []
        mx = []
        for m, metric in enumerate(metrics):
            # pos = x #+ (i - 1) * width
            for sched in schedulers:
                avg.append(results[setup][trace][sched][metric][0])
                std.append(results[setup][trace][sched][metric][1])
                mn.append(results[setup][trace][sched][metric][2])
                mx.append(results[setup][trace][sched][metric][3])
        # avg /= np.max(mx)
        # std /= np.max(mx)
        # mn /= np.max(mx)
        # mx /= np.max(mx)
        for i, _ in enumerate(metrics):
            avg[i * 3:(i + 1) * 3] /= np.max(mx[i * 3:(i + 1) * 3])
            std[i * 3:(i + 1) * 3] /= np.max(mx[i * 3:(i + 1) * 3])
            mn[i * 3:(i + 1) * 3] /= np.max(mx[i * 3:(i + 1) * 3])
            mx[i * 3:(i + 1) * 3] /= np.max(mx[i * 3:(i + 1) * 3])
        ax[t].text(0.25, np.max(mx[0:2]) + .05, "CP", horizontalalignment="center")
        ax[t].text(1.25, np.max(mx[3:6]) + .05 if not np.isnan(np.max(mx[3:6])) else 0.05, "WT", horizontalalignment="center")
        ax[t].text(2.25, np.max(mx[6:9]) + .05, "MS", horizontalalignment="center")
        rects1 = ax[t].bar(pos, mx, width, label="MAX")
        rects2 = ax[t].bar(pos, avg, width, label="AVG", yerr=std)
        rects3 = ax[t].bar(pos, mn, width, label="MIN")
        ax[t].set_xticklabels(xticks, rotation="vertical")
        ax[t].set_xticks(pos)
        ax[t].set_title(trace.capitalize())
        ax[t].set_ylim(0, 1.1)
        ax[t].set_yticks(np.arange(0, 1.1, 0.2))
        ax[t].grid(axis='y')


    # Add some text for labels, title and custom x-axis tick labels, etc.
    # ax.set_ylabel('Time')
    # ax.set_title('Average, Minimum and Maximum makespan of the different schedulers')
    # ax.set_xticks(x)
    # ax.set_xticklabels(results.keys())
    # ax.legend()
    # ax.grid(axis='y')
    fig.legend((rects1, rects2, rects3), ("MAX", "AVG", "MIN"))
    fig.suptitle("Job metrics for the " + prettySetups[s] + " for different traces and schedulers.")
    ax[0].set_ylabel("Normalized time")
    # plt.tight_layout()
    plt.gcf().subplots_adjust(bottom=0.15)
    plt.savefig(setup + "_job.pdf", format="pdf")
    # plt.show()

##############################################################################
###########             TASK METRICS                             #############
##############################################################################
results = {}
# for folder, name in exps:
for setup in setups:
    results[setup] = {}
    for trace in traces:
        results[setup][trace] = {}
        for sched in schedulers:
            results[setup][trace][sched] = {}
            with open("./results/" + trace + "_" + setup + "_" + sched + "/task_metrics.csv", 'r') as csvfile:
                rd = csv.DictReader(csvfile)
                ex = []
                wt = []
                ta = []
                for row in rd:
                    ex.append(int(row["execution"]))
                    wt.append(int(row["waiting"]))
                    ta.append(int(row["turnaround"]))
                results[setup][trace][sched]["EX"] = [np.mean(ex), np.std(ex), np.min(ex), np.max(ex)]
                results[setup][trace][sched]["WT"] = [np.mean(wt), np.std(wt), np.min(wt), np.max(wt)]
                results[setup][trace][sched]["TA"] = [np.mean(ta), np.std(ta), np.min(ta), np.max(ta)]


labels = ['AVG', 'MIN', 'MAX']

x = np.arange(len(labels))  # the label locations
width = 0.20  # the width of the bars

pos = [0, 0.25, 0.50, 1.0, 1.25, 1.5, 2, 2.25, 2.5]
metrics = ['EX', 'WT', 'TA']
xticks = []
for met in metrics:
    for sched in schedulers:
        xticks.append(sched)

for s, setup in enumerate(setups):
    fig, ax = plt.subplots(1, 3, figsize=(12,6))
    for t, trace in enumerate(traces):
        # ax = plt.subplot(1, 3, t + 1)
        avg = []
        std = []
        mn = []
        mx = []
        for m, metric in enumerate(metrics):
            # pos = x #+ (i - 1) * width
            for sched in schedulers:
                avg.append(results[setup][trace][sched][metric][0])
                std.append(results[setup][trace][sched][metric][1])
                mn.append(results[setup][trace][sched][metric][2])
                mx.append(results[setup][trace][sched][metric][3])
        for i, _ in enumerate(metrics):
            avg[i * 3:(i + 1) * 3] /= np.max(mx[i * 3:(i + 1) * 3])
            std[i * 3:(i + 1) * 3] /= np.max(mx[i * 3:(i + 1) * 3])
            mn[i * 3:(i + 1) * 3] /= np.max(mx[i * 3:(i + 1) * 3])
            mx[i * 3:(i + 1) * 3] /= np.max(mx[i * 3:(i + 1) * 3])
        ax[t].text(0.25, np.max(mx[0:2]) + .05, "EX", horizontalalignment="center")
        ax[t].text(1.25, np.max(mx[3:6]) + .05, "WT", horizontalalignment="center")
        ax[t].text(2.25, np.max(mx[6:9]) + .05, "TA", horizontalalignment="center")
        rects1 = ax[t].bar(pos, mx, width, label="MAX")
        rects2 = ax[t].bar(pos, avg, width, label="AVG", yerr=std)
        rects3 = ax[t].bar(pos, mn, width, label="MIN")
        ax[t].set_xticklabels(xticks, rotation="vertical")
        ax[t].set_xticks(pos)
        ax[t].set_title(trace.capitalize())
        ax[t].set_ylim(0, 1.1)
        ax[t].set_yticks(np.arange(0, 1.1, 0.2))
        ax[t].grid(axis='y')


    # Add some text for labels, title and custom x-axis tick labels, etc.
    # ax.set_ylabel('Time')
    # ax.set_title('Average, Minimum and Maximum makespan of the different schedulers')
    # ax.set_xticks(x)
    # ax.set_xticklabels(results.keys())
    # ax.legend()
    # ax.grid(axis='y')
    fig.legend((rects1, rects2, rects3), ("MAX", "AVG", "MIN"))
    fig.suptitle("Task metrics for the " + prettySetups[s] + " for different traces and schedulers.")
    ax[0].set_ylabel("Normalized time")
    # plt.tight_layout()
    plt.gcf().subplots_adjust(bottom=0.15)
    plt.savefig(setup + "_task.pdf", format='pdf')
    # plt.savefig(setup + "_task.png")
    # plt.show()
