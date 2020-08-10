# space-invaders-clj

Space Invaders emulation in Clojure.

## Usage

To play the game, clone this repository and do:

    lein run
    
### Controls

    c       insert coin
    1       1 player game start
    2       2 player game start (2 or more coins required)
    space   player 1 shoot
    left    player 1 move left
    right   player 1 move right
    
Controls for player 2 are handled by a different i/o port, and aren't yet emulated.

## License

Copyright © 2018-2020 Matt Grimm

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

## Acknowledgements

* This project owes a lot to [Emulator 101](http://www.emulator101.com) for inspiration and guidance. 