# -------------------------------------------------------------------------------
# HEIG-VD
#  Haute Ecole d'Ingenerie et de Gestion du Canton de Vaud
#  School of Business and Engineering in Canton de Vaud
# REDS Institute
#  Reconfigurable Embedded Digital Systems
# -------------------------------------------------------------------------------
# File      : run.tcl
# Authors   : christian.mueller@heig-vd.ch (CMR)
# Date      : 19.05.2014
#
# Context   : Logisim advanced simulator run script
#             You should run this script from comp file if you want the
#             the path to be correct (and don't mess up your sim folder).
#
# --| Modifications |------------------------------------------------------------
# Ver   Date        Engineer     Comments
# 0.0   See header  CMR          Initial version
# -------------------------------------------------------------------------------

puts "Run simulation environnment build..."

# Create a fresh physical library for the logical work library.
# Rebuilding it avoids reusing a stale or incomplete ModelSim/Questa library.
set logisim_worklib logisim_work
if {[file exists $logisim_worklib]} {
  file delete -force $logisim_worklib
}
vlib $logisim_worklib
vmap work $logisim_worklib

# Compile vhdl
do ../comp.tcl

# Start simulator
vsim top_sim

puts "Environnment builded"

# Start Logisim <-> questasim binder (that will drive simulation)
do ../questasim_binder.tcl $1
