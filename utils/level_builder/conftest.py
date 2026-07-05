import pytest


def pytest_addoption(parser):
    parser.addoption(
        '--interactive', action='store_true', default=False,
        help='Pause for user input before/after each step in the build scenario, '
             'so the TMX (and analyzer output) can be reviewed by hand. '
             'Off by default so CI never blocks on input.',
    )


@pytest.fixture
def interactive(request):
    return request.config.getoption('--interactive')
