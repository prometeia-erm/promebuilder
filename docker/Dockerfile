FROM continuumio/miniconda3:latest

ENV PYTHONVER=3.7
ENV PBENV=pbenv

# OS setup
RUN apt-get update && apt-get install -y jq

# Conda setup
RUN groupadd -r conda \
  && chgrp conda /opt/conda \
  && chmod g+rw /opt/conda
COPY condarc /etc/conda/.condarc
COPY pinned /opt/conda/conda-meta/pinned
RUN conda update --all -y \
  && conda clean --all -y
RUN conda info

# Changing to conda user
RUN useradd -r -m -g conda conda
USER conda

# Creating conda env
RUN conda create -y -q -n ${PBENV} python=${PYTHONVER}
RUN echo "python=${PYTHONVER}" > /home/conda/.conda/envs/${PBENV}/conda-meta/pinned

# Conda manual activation
ENV PATH="/home/conda/.conda/envs/${PBENV}/bin:${PATH}"
ENV CONDA_DEFAULT_ENV=${PBENV}
ENV CONDA_PREFIX=/home/conda/.conda/envs/${PBENV}

# Installing promebuilder
ARG PBPACKAGE
ENV PBPACKAGE=${PBPACKAGE}
RUN echo ${PBPACKAGE} | cut -d "-" -f 2 > /home/conda/pbver.txt
RUN if [ -z $(cat /home/conda/pbver.txt) ] || [ -z ${PBPACKAGE} ]; then exit 1; fi
COPY condarc.tmp /home/conda/.conda/envs/${PBENV}/.condarc
RUN conda install -y -n ${PBENV} promebuilder=$(cat /home/conda/pbver.txt) \
  && test -f /home/conda/.conda/pkgs/${PBPACKAGE} \
  && conda clean --all -y 
RUN rm /home/conda/.conda/envs/${PBENV}/.condarc
